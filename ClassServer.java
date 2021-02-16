import java.io.*;
import java.io.*;
import java.net.*;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;
import java.util.Scanner;
import javax.net.*;
import javax.net.ssl.*;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;
import javax.security.cert.X509Certificate;

/*
 * ClassServer.java -- a simple file server that can serve
 * Http get request in both clear and secure channel
 */

/**
 * Based on ClassServer.java in tutorial/rmi
 */
public abstract class ClassServer implements Runnable {

  private ServerSocket server = null;

  /**
   * Constructs a ClassServer based on <b>ss</b> and obtains a file's bytecodes
   * using the method <b>getBytes</b>.
   *
   */
  protected ClassServer(ServerSocket ss) {
    server = ss;
    newListener();
  }

  /**
   * Returns an array of bytes containing the bytes for the file represented by
   * the argument <b>path</b>.
   *
   * @return the bytes for the file
   * @exception FileNotFoundException if the file corresponding to <b>path</b>
   *                                  could not be loaded.
   * @exception IOException           if error occurs reading the class
   */
  public abstract byte[] getBytes(String path)
    throws IOException, FileNotFoundException;

  /**
   * The "listen" thread that accepts a connection to the server, parses the
   * header to obtain the file name and sends back the bytes for the file (or
   * error if the file is not found or the response was malformed).
   */
  public void run() {
    SSLSocket socket;
    X509Certificate cert;
    SSLSession session;
    // accept a connection
    try {
      socket = (SSLSocket) server.accept();
      session = (SSLSession) socket.getSession();
      cert = (X509Certificate) session.getPeerCertificateChain()[0];
    } catch (IOException e) {
      System.out.println("Class Server died: " + e.getMessage());
      e.printStackTrace();
      return;
    }

    String subject = cert.getSubjectDN().getName();
    String role = "P";
    String division = "1";
    subject = subject.substring(subject.indexOf("=") + 1, subject.indexOf(","));

    try {
      File empl = new File("empl.txt");
      BufferedReader reader = new BufferedReader(new FileReader(empl));

      while (reader.ready()) {
        String line = reader.readLine();
        String[] words = line.split(";");
        if (words[0].equals(subject)) {
          division = words[1];
          role = words[2];
        }
      }
    } catch (IOException e) {
      System.out.println("Class Server died: " + e.getMessage());
      e.printStackTrace();
      return;
    }

    // create a new thread to accept the next connection
    newListener();

    try {
      OutputStream rawOut = socket.getOutputStream();

      PrintWriter out = new PrintWriter(
        new BufferedWriter(new OutputStreamWriter(rawOut))
      );
      try {
        // get path to class file from header
        BufferedReader in = new BufferedReader(
          new InputStreamReader(socket.getInputStream())
        );

        String line = in.readLine();

        String path = getPath(line);
        String action = getAction(line);
        String newCont = "";
        if (action.equalsIgnoreCase("write")) {
          newCont = getCont(in);
        }
        // retrieve bytecodes
        byte[] bytecodes = getBytes(path);
        // send bytecodes in response (assumes HTTP/1.0 or later)

        if (isAuthorised(subject, division, role, bytecodes, path, action)) {
   
          handleAction(rawOut, bytecodes, action, path, out, newCont);
        } else {
          out.println("HTTP/1.0 400 Unauthorised! \r\n");
          out.println("Content-Type: text/html\r\n\r\n");
          out.flush();
        }
      } catch (Exception e) {
        e.printStackTrace();
        // write out error response
        out.println("HTTP/1.0 400 " + e.getMessage() + "\r\n");
        out.println("Content-Type: text/html\r\n\r\n");
        out.flush();
      }
    } catch (IOException ex) {
      // eat exception (could log error to log file, but
      // write out to stdout for now).
      System.out.println("error writing response: " + ex.getMessage());
      ex.printStackTrace();
    } finally {
      try {
        socket.close();
      } catch (IOException e) {}
    }
  }

  /**
   * Create a new thread to listen.
   */
  private void newListener() {
    (new Thread(this)).start();
  }

  private boolean handleAction(
    OutputStream rawOut,
    byte[] bytecodes,
    String action,
    String path,
    PrintWriter out,
    String newCont
  ) {
    switch (action.toLowerCase()) {
      case "read":
        out.println("File...\r\n");
        out.flush();
        try {
          rawOut.write(bytecodes);
          rawOut.flush();
        } catch (IOException ie) {
          ie.printStackTrace();
        }
        break;
      case "write":
        out.println("File before writing...\r\n");
        out.flush();
        try {
          rawOut.write(bytecodes);
        } catch (IOException ie) {
          ie.printStackTrace();
        }

        if (WriteToFile(path, newCont)) {
          out.println("File after writing...\r\n");
          out.flush();

          try {
            bytecodes = getBytes(path);

            rawOut.write(bytecodes);
            rawOut.flush();
          } catch (IOException ie) {
            ie.printStackTrace();
          }
        }

        break;
      case "create file":
        break;
    }
    return false;
  }

  private Boolean WriteToFile(String path, String newCont) {
    try {
      File myFile = new File(path); // Specify the filename
      StringBuilder builder = new StringBuilder();
      Scanner scannerF = new Scanner(new File(path));

      int count = 0;
      while (scannerF.hasNextLine() && count < 3) {
        String line = scannerF.nextLine();
        builder.append(line + "\n");
        count++;
      }
      FileWriter myWriter = new FileWriter(path);
      myWriter.write(builder.toString());
      myWriter.write(newCont);
      myWriter.close();

      return true;
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
      return false;
    }
  }

  private static String getCont(BufferedReader in) throws IOException {
    StringBuilder sb = new StringBuilder();

    String line = in.readLine();

    while (
      line != null &&
      line.length() != 0 &&
      (line.charAt(0) != '\r') &&
      (line.charAt(0) != '\n')
    ) {
      sb.append(line + "\n");
      line = in.readLine();
    }

    return sb.toString();
  }

  /**
   * Returns the path to the file obtained from parsing the HTML header.
   */
  private static String getPath(String line) throws IOException {
    String path = "";
    // extract class from GET line
    if (line.startsWith("GET /")) {
      line = line.substring(5, line.length() - 1).trim();
      int index = line.indexOf(' ');
      if (index != -1) {
        path = line.substring(0, index);
      }
    }
    return path;
  }

  private static String getAction(String line) throws IOException {
    String path = "";
    // extract class from GET line
    if (line.startsWith("GET /")) {
      line = line.substring(line.indexOf('@') + 1, line.length() - 1).trim();
      int index = line.indexOf(' ');
      if (index != -1) {
        path = line.substring(0, index);
      }
    }
    return path;
  }

  private static Boolean isAuthorised(
    String name,
    String division,
    String role,
    byte[] fileContent,
    String path,
    String action
  ) {
    String[] content = new String(fileContent).split("\n");
    //String nurse = content[0];
    //String doctor = content[1];
    //String division = content[2];
    switch (role) {
      case "S":
        if (content[0].equalsIgnoreCase(name)) {
          if (
            action.equalsIgnoreCase("read") || action.equalsIgnoreCase("write")
          ) {
            return true;
          }

          return false;
        } else if (content[2].equalsIgnoreCase(division)) {
          if (action.equalsIgnoreCase("read")) {
            return true;
          }
          return false;
        }
        break;
      case "D":
        if (content[1].equalsIgnoreCase(name)) {
          if (
            action.equalsIgnoreCase("read") || action.equalsIgnoreCase("write")
          ) {
            return true;
          }

          return false;
        } else if (content[2].equalsIgnoreCase(division)) {
          if (action.equalsIgnoreCase("read")) {
            return true;
          }
          return false;
        }

        break;
      case "P":
        if (path.contains(name)) {
          if (action.equalsIgnoreCase("read")) {
            return true;
          }
          return false;
        }
        break;
      //Testa att skriva til filer
    }
    return false;
  }
}
