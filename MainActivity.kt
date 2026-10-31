package com.printqueue.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);setContent { MaterialTheme { PrintScreen() } } }
}

data class PickedFile(val uri: Uri,val name:String,val mode:String="BW",val copies:Int=1,val pages:Int=1)
private fun documentName(context:Context,uri:Uri):String {
 context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c ->
  if(c.moveToFirst())return c.getString(0)?:"document"
 }
 return "document"
}
private fun base(input:String):String { val b=input.trim().trimEnd('/');require(b.startsWith("http://")||b.startsWith("https://"));return if(b.endsWith("/api"))b else "$b/api" }
private fun auth(username:String,password:String):String="Basic "+Base64.encodeToString("${username.lowercase()}:$password".toByteArray(Charsets.UTF_8),Base64.NO_WRAP)
private fun request(url:String,method:String="GET",username:String="",password:String="",json:String?=null):String {
 val c=URL(url).openConnection() as HttpURLConnection
 try {
  c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=60000
  if(username.isNotBlank())c.setRequestProperty("Authorization",auth(username,password))
  if(json!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(json.toByteArray())}}
  val text=(if(c.responseCode in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
  if(c.responseCode !in 200..299)error("HTTP ${c.responseCode}: ${text.take(160)}")
  return text
 } finally { c.disconnect() }
}
private fun upload(context:Context,endpoint:String,user:String,pass:String,items:List<PickedFile>,payment:String):JSONObject {
 val boundary="PrintQueue-${UUID.randomUUID()}";val c=URL("${base(endpoint)}/customer/orders").openConnection() as HttpURLConnection
 try {
  c.requestMethod="POST";c.connectTimeout=15000;c.readTimeout=120000;c.doOutput=true;c.setChunkedStreamingMode(8192)
  c.setRequestProperty("Authorization",auth(user,pass));c.setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary")
  BufferedOutputStream(c.outputStream).use { out ->
   fun write(s:String){out.write(s.toByteArray(Charsets.UTF_8))}
   fun field(name:String,value:String){write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")}
   field("paymentMethod",payment)
   for(f in items){
    field("modes",f.mode);field("copies",f.copies.toString());field("pages",f.pages.toString())
    val safe=f.name.replace("\"","_").replace("\r","_").replace("\n","_")
    write("--$boundary\r\nContent-Disposition: form-data; name=\"files\"; filename=\"$safe\"\r\nContent-Type: application/octet-stream\r\n\r\n")
    context.contentResolver.openInputStream(f.uri)?.use{it.copyTo(out)}?:error("Cannot read $safe")
    write("\r\n")
   }
   write("--$boundary--\r\n")
  }
  val text=(if(c.responseCode in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
  if(c.responseCode !in 200..299)error("HTTP ${c.responseCode}: ${text.take(180)}")
  return JSONObject(text)
 }finally{c.disconnect()}
}

@Composable fun PrintScreen(){
 val context=LocalContext.current;val activity=context as ComponentActivity
 var endpoint by remember{mutableStateOf("https://print.example.com")};var user by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var displayName by remember{mutableStateOf("")}
 var loggedIn by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var files by remember{mutableStateOf(listOf<PickedFile>())}
 var shopName by remember{mutableStateOf("My Print Shop")};var shopLogo by remember{mutableStateOf("")}
 var payment by remember{mutableStateOf("PAY_AT_SHOP")};var orders by remember{mutableStateOf(JSONArray())};var bwRate by remember{mutableStateOf(2.0)};var colorRate by remember{mutableStateOf(10.0)};var maxFiles by remember{mutableIntStateOf(10)};var maxFileMb by remember{mutableIntStateOf(15)}
 val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris ->
  if(uris.size+files.size>maxFiles)message="Up to $maxFiles files per order"
  else {
   val tooLarge=uris.any { uri -> context.contentResolver.query(uri,arrayOf(OpenableColumns.SIZE),null,null,null)?.use { c ->
     c.moveToFirst() && !c.isNull(0) && c.getLong(0) > maxFileMb.toLong()*1024*1024
    } ?: false }
   if(tooLarge)message="A selected file exceeds $maxFileMb MB"
   else files=files+uris.map{PickedFile(it,documentName(context,it))}
  }
 }
 fun runTask(action:()->Unit){busy=true;thread{try{action()}catch(e:Exception){activity.runOnUiThread{message=e.message?:"Network error"}}finally{activity.runOnUiThread{busy=false}}}}
 fun fetchRates(){val url=endpoint;val u=user;val p=pass;runTask{
  val rates=JSONObject(request("${base(url)}/customer/pricing",username=u,password=p))
  val limits=JSONObject(request("${base(url)}/customer/upload-settings",username=u,password=p))
  val branding=JSONObject(request("${base(url)}/customer/shop-branding",username=u,password=p))
  activity.runOnUiThread{shopName=branding.optString("shopName","My Print Shop");shopLogo=branding.optString("logoDataUrl","")}
  activity.runOnUiThread{bwRate=rates.optDouble("bwRate",2.0);colorRate=rates.optDouble("colorRate",10.0);maxFiles=limits.optInt("maxFiles",10);maxFileMb=limits.optInt("maxFileMb",15)}
 }}
 fun refresh(){val u=user;val p=pass;val url=endpoint;runTask{val value=JSONArray(request("${base(url)}/customer/orders",username=u,password=p));activity.runOnUiThread{orders=value;message="Orders refreshed"}}}
 Scaffold { padding -> Column(Modifier.padding(padding).padding(18.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(shopName,style=MaterialTheme.typography.headlineLarge,modifier=Modifier.weight(1f))
   if(shopLogo.startsWith("data:image/")) {
    val bitmap=remember(shopLogo){runCatching {val bytes=Base64.decode(shopLogo.substringAfter(","),Base64.DEFAULT);BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap()}.getOrNull()}
    if(bitmap!=null)Image(bitmap,contentDescription="Shop logo",modifier=Modifier.size(65.dp))
   }
  }
  OutlinedTextField(endpoint,{endpoint=it},label={Text("Server URL")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  if(!loggedIn){
   OutlinedTextField(user,{user=it},label={Text("Username")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(pass,{pass=it},label={Text("Password (min 8 for register)")},modifier=Modifier.fillMaxWidth(),visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation())
   OutlinedTextField(displayName,{displayName=it},label={Text("Display name (register only)")},modifier=Modifier.fillMaxWidth())
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Button(enabled=!busy&&user.isNotBlank()&&pass.isNotBlank(),onClick={val u=user;val p=pass;val url=endpoint;runTask{request("${base(url)}/customer/orders",username=u,password=p)
     val limits=JSONObject(request("${base(url)}/customer/upload-settings",username=u,password=p))
     val branding=JSONObject(request("${base(url)}/customer/shop-branding",username=u,password=p))
     activity.runOnUiThread{shopName=branding.optString("shopName","My Print Shop");shopLogo=branding.optString("logoDataUrl","")}
     activity.runOnUiThread{maxFiles=limits.optInt("maxFiles",10);maxFileMb=limits.optInt("maxFileMb",15);loggedIn=true;message="Signed in"}}}){Text("Sign in")}
    OutlinedButton(enabled=!busy&&user.isNotBlank()&&pass.length>=8,onClick={val body=JSONObject().put("username",user).put("password",pass).put("displayName",displayName).toString();val url=endpoint;runTask{request("${base(url)}/auth/register",method="POST",json=body);activity.runOnUiThread{message="Account created; sign in now"}}}){Text("Register")}
   }
  }else{
   Text("Signed in as $user")
   OutlinedButton(enabled=!busy,onClick={fetchRates()}){Text("Refresh printing prices")}
   Text("Rates: B&W ₹$bwRate/page · Color ₹$colorRate/page")
   Text("Upload limit: $maxFiles files, $maxFileMb MB each")
   OutlinedButton(onClick={loggedIn=false;pass="";files=emptyList();orders=JSONArray()}){Text("Sign out")}
   Button(onClick={pick.launch(arrayOf("application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","image/jpeg","image/png"))},enabled=!busy){Text("Add documents (${files.size}/$maxFiles)")}
   files.forEachIndexed{index,f->
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
     Text(f.name);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      FilterChip(selected=f.mode=="BW",onClick={files=files.toMutableList().also{it[index]=f.copy(mode="BW")}},label={Text("B&W")})
      FilterChip(selected=f.mode=="COLOR",onClick={files=files.toMutableList().also{it[index]=f.copy(mode="COLOR")}},label={Text("Color")})
     }
     Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
      Text("Pages: ${f.pages}");OutlinedButton(onClick={files=files.toMutableList().also{it[index]=f.copy(pages=(f.pages-1).coerceAtLeast(1))}}){Text("−")}
      OutlinedButton(onClick={files=files.toMutableList().also{it[index]=f.copy(pages=(f.pages+1).coerceAtMost(10000))}}){Text("+")}
     }
     Text("Estimated ₹%.2f".format(java.util.Locale.US,(if(f.mode=="BW")bwRate else colorRate)*f.pages*f.copies))
     Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
      Text("Copies: ${f.copies}");OutlinedButton(onClick={files=files.toMutableList().also{it[index]=f.copy(copies=(f.copies-1).coerceAtLeast(1))}}){Text("−")}
      OutlinedButton(onClick={files=files.toMutableList().also{it[index]=f.copy(copies=(f.copies+1).coerceAtMost(100))}}){Text("+")}
      OutlinedButton(onClick={files=files.toMutableList().also{it.removeAt(index)}}){Text("Remove")}
     }
    }}
   }
   Text("Estimated total: ₹%.2f".format(java.util.Locale.US,files.sumOf{(if(it.mode=="BW")bwRate else colorRate)*it.pages*it.copies}))
   Text("Enter the actual number of pages per document; auto-counting is not implemented. Final prices are calculated by server when ordered.")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    FilterChip(payment=="PAY_AT_SHOP",onClick={payment="PAY_AT_SHOP"},label={Text("Pay at shop")})
    FilterChip(payment=="ONLINE",onClick={payment="ONLINE"},label={Text("Online (not charged)")})
   }
   Button(enabled=!busy&&files.isNotEmpty(),onClick={val snapshot=files.toList();val u=user;val p=pass;val url=endpoint;val pay=payment;runTask{
    val result=upload(context,url,u,p,snapshot,pay);activity.runOnUiThread{message="Uploaded! Token ${result.optString("token")} · ₹${result.optString("totalAmount")}";files=emptyList()}
    val latest=JSONArray(request("${base(url)}/customer/orders",username=u,password=p));activity.runOnUiThread{orders=latest}
   }},modifier=Modifier.fillMaxWidth()){Text(if(busy)"Uploading..." else "Upload all & get token")}
   OutlinedButton(enabled=!busy,onClick={refresh()}){Text("Refresh my orders")}
   for(i in 0 until orders.length()){
    val o=orders.optJSONObject(i)?:continue
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
     Text("Token ${o.optString("token")}",style=MaterialTheme.typography.titleMedium)
     Text("Status: ${o.optString("status")} · Amount: ₹${o.optString("totalAmount", "Legacy")}");val fs=o.optJSONArray("files")?:JSONArray()
     OutlinedButton(enabled=!busy,onClick={
      val orderId=o.optLong("id");val u=user;val pw=pass;val url=endpoint
      runTask{
       request("${base(url)}/customer/orders/$orderId/documents",method="DELETE",username=u,password=pw)
       val latest=JSONArray(request("${base(url)}/customer/orders",username=u,password=pw))
       activity.runOnUiThread{orders=latest;message="All documents in this token deleted globally"}
      }
     }){Text("Delete entire set (both sides)")}
     for(k in 0 until fs.length()){val item=fs.optJSONObject(k)?:continue;Text("• ${item.optString("name")} · ${item.optString("mode")} × ${item.optInt("copies")}")
      OutlinedButton(enabled=!busy,onClick={
       val orderId=o.optLong("id");val fileId=item.optLong("id");val u=user;val pw=pass;val url=endpoint
       runTask{
        request("${base(url)}/customer/orders/$orderId/files/$fileId",method="DELETE",username=u,password=pw)
        val latest=JSONArray(request("${base(url)}/customer/orders",username=u,password=pw))
        activity.runOnUiThread{orders=latest;message="Document deleted from customer and owner views"}
       }
      }){Text("Delete document (both sides)")}}
    }}
   }
  }
  if(message.isNotBlank())Text(message)
  Text("Local dev only. Owner-only file downloads; online payments are NOT processed. Use HTTPS before production.",style=MaterialTheme.typography.bodySmall)
 }}
}
