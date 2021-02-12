# !/bin/bash
read -p "enter name of client: " cn
clientName=$(echo "$cn" | tr '[:upper:]' '[:lower:]')
#2
keytool -importcert -file ca.crt -keystore truststore.jks -alias ca -storepass password

#3
keytool -genkeypair -keystore keystore.jks -alias clientkey -storepass password

#4
keytool -certreq -file req.csr -keystore keystore.jks -alias clientkey -storepass password

#5
openssl x509 -req -in req.csr -out req.signed -CA ca.crt -CAkey ca.key -CAcreateserial

#6
keytool -importcert -file ca.crt -keystore keystore.jks -alias ca -storepass password

keytool -importcert -file req.signed -keystore keystore.jks -alias clientkey -storepass password
mkdir -p "clients/$clientName"
mv truststore.jks keystore.jks   "clients/$clientName" 
