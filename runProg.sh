javac server.java client.java
java server 9876  TLS true &
java client localhost 9876 /text.txt