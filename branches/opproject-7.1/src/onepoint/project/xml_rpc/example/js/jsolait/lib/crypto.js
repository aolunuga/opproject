
Module("crypto","$Revision: 5 $",function(mod){
mod.listEncrypters=function(){
var c=[];
for(var attr in String.prototype){
if(attr.slice(0,8)=="encrypt_"){
c.push(attr.slice(8));
}
}
return c;
};
mod.listDecrypters=function(){
var c=[];
for(var attr in String.prototype){
if(attr.slice(0,8)=="decrypt_"){
c.push(attr.slice(8));
}
}
return c;
};
String.prototype.encrypt=function(crydec){
var n="encrypt_"+crydec;
if(String.prototype[n]){
var args=[];
for(var i=1;i<arguments.length;i++){
args[i-1]=arguments[i];
}
return String.prototype[n].apply(this,args);
}else{
throw new mod.Exception("Decrypter '%s' not found.".format(crydec));
}
};
String.prototype.decrypt=function(crydec){
var n="decrypt_"+crydec;
if(String.prototype[n]){
var args=[];
for(var i=1;i<arguments.length;i++){
args[i-1]=arguments[i];
}
return String.prototype[n].apply(this,args);
}else{
throw new mod.Exception("Encrypter '%s' not found.".format(crydec));
}
};
String.prototype.encrypt_xor=function(key){
var e=new Array(this.length);
var l=key.length;
for(var i=0;i<this.length;i++){
e[i]=String.fromCharCode(this.charCodeAt(i)^key.charCodeAt(i%l));
}
return e.join("");
};
String.prototype.decrypt_xor=String.prototype.encrypt_xor;
String.prototype.encrypt_rc4=function(key){
var sbox=new Array(256);
for(var i=0;i<256;i++){
}
var j=0;
for(var k=0;k<this.length;k++){
String.prototype.decrypt_rc4=String.prototype.encrypt_rc4;
});