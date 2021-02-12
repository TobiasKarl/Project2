
#1
keytool -genkeypair -keystore serverkeystore.jks -alias serverkey -storepass password  \
-dname "CN=MyServer, OU=D, O=LTH, L=Lund, S=Scania, C=SE"

#2
keytool -certreq -file serverreq.csr -keystore serverkeystore.jks -alias serverkey -storepass password

#3
openssl x509 -req -in serverreq.csr -out serverreq.signed -CA ca.crt -CAkey ca.key -CAcreateserial

#4
keytool -importcert -file ca.crt -keystore serverkeystore.jks -alias ca -storepass password

keytool -importcert -file serverreq.signed -keystore serverkeystore.jks -alias serverkey -storepass password

#5
keytool -importcert -file ca.crt -keystore servertruststore.jks -alias ca -storepass password