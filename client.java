import java.io.*;
import java.io.Console;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

/*
 * This example shows how to set up a key manager to perform client
 * authentication.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class client {

  public static void main(String[] args) throws Exception {
    String host = null;
    int port = -1;
    String path = null;
    Console cnsl = System.console();
    for (int i = 0; i < args.length; i++) System.out.println(args[i]);

    if (args.length < 3) {
      System.out.println(
        "USAGE: java SSLSocketClientWithClientAuth " +
        "host port requestedfilepath"
      );
      System.exit(-1);
    }

    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
      path = args[2];
    } catch (IllegalArgumentException e) {
      System.out.println("USAGE: java client host port");
      System.exit(-1);
    }
    String myname = "";
    try {/* set up a key manager for client authentication */
      SSLSocketFactory factory = null;
      try {
        System.out.println("What is your name?");
        Scanner scanner = new Scanner(System.in);
        myname = cnsl.readLine("Name: ").toLowerCase();

        System.out.println("Enter password: ");
        char[] password = cnsl.readPassword("Password: ");

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        SSLContext ctx = SSLContext.getInstance("TLS");
        ks.load(
          new FileInputStream("clients/" + myname + "/keystore.jks"),
          password
        ); // keystore password (storepass)
        ts.load(
          new FileInputStream("clients/" + myname + "/keystore.jks"),
          password
        ); // truststore password (storepass);
        kmf.init(ks, password); // user password (keypass)
        tmf.init(ts); // keystore can be used as truststore here
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        factory = ctx.getSocketFactory();
      } catch (Exception e) {
        System.out.println("Name or password is wrong. Try again!");
        return;
      }
      SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
      System.out.println("\nsocket before handshake:\n" + socket + "\n");
      System.out.println("\nversion-handshake:\n" + socket + "\n");

      boolean okAction = false;
      String action = "";
      while (!okAction) {
        System.out.println(
          "What action would you like to do? Options are: Read, Write, Create file or Delete"
        );
        Scanner scanner = new Scanner(System.in);
        action = scanner.nextLine().toLowerCase();
        if (
          action.equalsIgnoreCase("write") ||
          action.equalsIgnoreCase("read") ||
          action.equalsIgnoreCase("create file")||
          action.equalsIgnoreCase("delete")
        ) {
          okAction = true;
        }
      }

      /*
       * send http request
       *
       * See SSLSocketClient.java for more information about why
       * there is a forced handshake here when using PrintWriters.
       */

      socket.startHandshake();
      PrintWriter out = new PrintWriter(
        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
      );
      String cont = "";

      if (action.equalsIgnoreCase("create file")) {
        System.out.println("What is the name of the patient?");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine().toLowerCase();
        out.println("CREATE /" + name + "# HTTP/1.0");

        System.out.println("What is the name of the nurse?");
        Scanner nurseN = new Scanner(System.in);
        String nameNur = nurseN.nextLine().toLowerCase();

        System.out.println("What department?");
        Scanner depS = new Scanner(System.in);
        String dep = depS.nextLine().toLowerCase();

        System.out.println("What would you like to write to the file?");
        Scanner scannerC = new Scanner(System.in);
        cont = scannerC.nextLine().toLowerCase();
        out.println(nameNur);
        out.println(myname);
        out.println(dep);
      } else {
        System.out.println("Which patient?");
        Scanner patSC = new Scanner(System.in);
        String pat = patSC.nextLine().toLowerCase();
        System.out.println("What journal number?");
        Scanner patSN = new Scanner(System.in);
        String patN = patSN.nextLine().toLowerCase();
        path = "/clients/" + pat + "/journal" + patN + ".txt";
        out.println("GET " + path + "# @" + action + " HTTP/1.0");
      }
      if (action.equalsIgnoreCase("write")) {
        System.out.println("What would you like to write to the file?");
        Scanner scannerC = new Scanner(System.in);
        cont = scannerC.nextLine().toLowerCase();
      }

      out.println(cont);
      out.println();
      out.flush();
      SSLSession session = socket.getSession();
      X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
      String subject = cert.getIssuerDN().getName();
      System.out.println(
        "certificate name (subject DN field) on certificate received from server:\n" +
        subject +
        "\n"
      );
      System.out.println("Issuer field:" + cert.getIssuerDN() + "\n");
      System.out.println("Serial number:" + cert.getSerialNumber() + "\n");
      System.out.println("socket after handshake:\n" + socket + "\n");
      System.out.println("secure connection established\n\n");
      String name = cert.getIssuerDN().getName();

      if (out.checkError()) System.out.println(
        "SSLSocketClient: java.io.PrintWriter error"
      );

      /* read response */
      BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      );

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        System.out.println(inputLine);
      }
      in.close();
      out.close();
      socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
