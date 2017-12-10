import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class Crawler {

    HashSet<String> links;
    String homeURL;         //The homepage of the website to be crawled
    ArrayList<URI> visited; //Stores URIs of each web page visited

    /* Parameterized Constructor */
    public Crawler(String homeURL) {

        this.homeURL = homeURL;
        links = new HashSet();
        visited = new ArrayList<>();
    }

    /* Utility caller method */
    public void crawl() {
        processPage1(homeURL);
    }

    /* This method follows the approach of using Regular Expressions to verify websites(Not Working)*/
    public void processPage(String url) {

        //Regex to check whether it is of the home URL
        url = transformURL(url);
        System.out.println("Crawling " + url + "...");

        if (url != null && !links.contains(url)) {
            links.add(url);
            try {
                Connection connection = Jsoup.connect(url);

                //Getting Status code
                Connection.Response response = connection.response();
                int statusCode = response.statusCode();

                if (statusCode == 0) {

                    Document htmlDoc = connection.get();
                    Elements aElements = htmlDoc.select("a[href]");
                    for (Element e : aElements) {
                        processPage(e.attr("href"));
                    }
                } else {
                    System.out.println(url + " is not accessible as status code is " + statusCode + "and Status message is " + response.statusMessage());
                }
            } catch (IOException ex) {
                System.out.print("\t-------IOException");
            } catch (IllegalArgumentException ex) {
                System.out.print("\t------Illegal Argument");
            }

        } else {
            System.out.println(url + " already accessed!");

        }
    }

    /* This method uses Java Libraries to do the most work */
    /* Using BFS, I traversed all the pages */
    public void processPage1(String url) {

        //Regex to check whether it is of the home URL

        URI uri = null;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            System.out.println("--------wrong URL");
        }


        Queue<URI> queue = new LinkedList<>();
        queue.offer(uri);
        while (true) {

            int nodeCount = queue.size();
            if(nodeCount == 0)
                return;

            while(nodeCount-->0) {

                URI temp = queue.poll();

                //If the URL is already visited
                if(isVisited(temp)) {
                    //System.out.println("visited!");
                    continue;
                }

                String tempURI = temp.toASCIIString();
                System.out.println("processPage: "+tempURI);
                processHTML(tempURI, queue);
                visited.add(temp);
                //System.out.println("Added: "+temp.toASCIIString());
            }
        }
    }

    /* Processes each HTML page using Jsoup in the queue and extracts href attribute of <a> tags from the web page */
    public void processHTML(String url, Queue<URI> queue) {
        Connection connection = null;
        try {
            connection = Jsoup.connect(url);
        } catch (IllegalArgumentException ex) {
            System.out.print("\t------Illegal Argument");
            //return;
        } catch (NullPointerException ex) {
            System.out.print("\t------NullPointerException");
            //return;
        }
        //Getting Status code
        int statusCode = connection.response().statusCode();

        if (statusCode == 0) {

            Document htmlDoc = null;
            try {
                htmlDoc = connection.get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements aElements = htmlDoc.select("a[href]");
            for (Element e : aElements) {
                URI u1 = URI.create(e.attr("href"));
                if(!u1.isAbsolute() && u1.toASCIIString().startsWith("/")) {
                    URL baseURL = null;
                    try {
                        baseURL = new URL(url);
                        URL url1 = new URL(baseURL, u1.toASCIIString());
                        u1 = url1.toURI();
                    } catch (URISyntaxException e1) {
                        e1.printStackTrace();
                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                    //System.out.println("processHTML: "+u1.toASCIIString());
                }
                if(!u1.toASCIIString().contains("did-you-knows"))
                    continue;
                queue.offer(u1);
            }
        }
    }

    /* Checks whether the URL is visited or not to avoid going in a loop */
    public boolean isVisited(URI uri) {

        for(URI u: visited) {
            if(u.equals(uri))
                return true;
        }
        return false;
    }

    /* Checks the URL */
    public int checkURL(String url) {
        String absoluteURL = "(https?://)?" + "(www\\.)?" + "[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\." + "[a-z]{2,6}" + "\\b([-a-zA-Z0-9@:%_\\+.~#?&//=])*";
        boolean absoluteMatch = Pattern.matches(absoluteURL, url);

        String relativeURL = "^(/|[a-z0-9-_])*" + "\\.[a-z]*$";
        boolean relativeMatch = Pattern.matches(relativeURL, url);

        if (absoluteMatch) {
            if (url.contains("did-you-knows"))
                return 1;
            return 0;
        } else if (relativeMatch) {
            return 2;
        } else {
            return 0;
        }
    }

    /* Transforms the URL Relative->Absolute */
    public String transformURL(String url) {
        int urlCode = checkURL(url);
        String newURL = null;
        if (urlCode == 1) {
            //Correct the absolute url
            newURL = url;
        } else if (urlCode == 2) {
            //change relative to absolute
            newURL = homeURL + url;
        }
        return newURL;
    }

    /* Main method */
    public static void main(String[] args) throws IOException {

        String homeURL = "http://www.did-you-knows.com/";
        Crawler crawler = new Crawler(homeURL);
        crawler.crawl();
    }
}
