package us.rockhopper.spider;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Charlotte, the web crawler.
 * 
 * @author Tim Clancy
 * @version 9.7.15 - support for both DFS and BFS crawling, capable of storing
 *          graph data in MySQL database, capable of selecting a random number
 *          of links per page in order to achieve more artistic branching.
 * 
 *          Acknowledgments: Charlotte's DFS code was learned from ryanlr's
 *          excellent tutorial at
 *          http://www.programcreek.com/2012/12/how-to-make-a-web-crawler-using-
 *          java/. I am also grateful for the use of JSoup.
 */
public class Charlotte {

	// The maximum number of links to randomly follow per page.
	private static final int MAX_LINKS = 10;
	private static Database db = new Database();
	private static int nextID = 0;

	public static void main(String[] args) throws SQLException, IOException {
		// Clear the databases.
		String query = "TRUNCATE record;";
		PreparedStatement state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		state.execute();
		query = "TRUNCATE edges;";
		state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		state.execute();

		// The seed page:
		String seed = "http://www.una-gp.org/";
		processPageBFS(seed);
	}

	/**
	 * Takes a URL and removes its trailing slash.
	 * 
	 * @param URL
	 *            the URL to trim.
	 * @return The input URL trimmed of trailing slash.
	 */
	private static String trimTrailingSlash(String URL) {
		if (URL.length() == 0) {
			return URL;
		}
		if (URL.charAt(URL.length() - 1) == '/') {
			return URL.substring(0, URL.length() - 1);
		}
		return URL;
	}

	/**
	 * Sanitizes a URL for further processing in search and database storage.
	 * 
	 * @param URL
	 *            the URL to clean.
	 * @return The input URL, cleaned of http header, page anchors, and most GET
	 *         data.
	 */
	private static String sanitizeURL(String URL) {
		int i = URL.indexOf("http:/");
		int j = URL.indexOf("https://");
		int t = URL.lastIndexOf("#");
		int q = URL.indexOf("?");

		int h;
		if (q == -1) {
			h = t;
		} else {
			h = q;
		}

		if (i == -1 && j == -1) {
			return trimTrailingSlash(URL);
		}

		if (i == -1 && h == -1) {
			return trimTrailingSlash(URL.substring(j + 8));
		}

		if (i == -1 && h != -1) {
			return trimTrailingSlash(URL.substring(j + 8, h));
		}

		if (j == -1 && h == -1) {
			return trimTrailingSlash(URL.substring(i + 7));
		}

		if (j == -1 && h != -1) {
			return trimTrailingSlash(URL.substring(i + 7, h));
		}

		return trimTrailingSlash(URL);
	}

	/**
	 * Processes the web according to depth-first search. In practice, this will
	 * just follow the first link of every page and create a visualization
	 * which, while suitable for showing in a straight line the huge size of the
	 * Internet, is otherwise boring.
	 * 
	 * @param URL
	 *            the URL to process for links.
	 */
	public static void processPageDFS(String URL) throws SQLException, IOException {
		// Sanitize the URL.
		String sanURL = sanitizeURL(URL);

		// Store the URL to prevent further processing.
		String query = "INSERT INTO `web_crawler`.`record` " + "(`URL`) VALUES " + "(?);";
		PreparedStatement state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		state.setString(1, sanURL);
		state.execute();

		// Find this URL's ID.
		String IDquery = "SELECT `RecordID` FROM record WHERE URL = '" + sanURL + "'";
		ResultSet rsID = db.executeQuery(IDquery);
		rsID.next();
		int fromID = rsID.getInt("RecordID");
		nextID = fromID;
		nextID++;

		// Print the name of the newly-found page.
		System.out.println(sanURL);

		// Get page information.
		Document doc;
		try {
			doc = Jsoup.connect(URL).get();
		} catch (Exception e) {
			System.out.println("Encountered an invalid link, skipping.");
			return;
		}

		// Get all links recursively.
		Elements links = doc.select("a[href]");
		for (Element link : links) {
			String newURL = link.attr("abs:href");
			String sanNewURL = sanitizeURL(newURL);

			// Check to make sure there are no self-loops.
			if (sanNewURL.equals(sanURL)) {
			} else {
				int toID = nextID;

				// Check if the given URL is already in database.
				query = "SELECT `RecordID` FROM record WHERE URL = '" + sanNewURL + "'";
				ResultSet rs = db.executeQuery(query);
				boolean seen = false;
				if (rs.next()) {
					toID = rsID.getInt("RecordID");
					seen = true;
				} else {
					nextID++;
				}

				// Add edges to this URL into our database.
				query = "INSERT INTO `web_crawler`.`edges` " + "(`from`, `to`) VALUES " + "(?, ?);";
				state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				state.setString(1, fromID + "");
				state.setString(2, toID + "");
				state.execute();

				if (!seen) {
					processPageDFS(newURL);
				}
			}
		}
	}

	/**
	 * Processes the web according to breadth-first search. This results in a
	 * much more interesting visualization of the web, especially when the
	 * amount of branching is restricted per URL. This is also a more
	 * interesting tool for finding some properties of the section of the
	 * Internet adjacent to the seed URL.
	 * 
	 * @param URL
	 *            the URL to process for links.
	 */
	public static void processPageBFS(String URL) throws SQLException, IOException {
		// Sanitize the URL.
		String sanURL = sanitizeURL(URL);

		// Store the URL to prevent further processing.
		String query = "INSERT INTO `web_crawler`.`record` " + "(`URL`) VALUES " + "(?);";
		PreparedStatement state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		state.setString(1, sanURL);
		state.execute();

		// Find this URL's ID.
		String IDquery = "SELECT `RecordID` FROM record WHERE URL = '" + sanURL + "'";
		ResultSet rsID = db.executeQuery(IDquery);
		rsID.next();
		int fromID = rsID.getInt("RecordID");

		// Print the name of the newly-found page.
		System.out.println(sanURL);

		// Get page information.
		Document doc;
		try {
			doc = Jsoup.connect(URL).get();
		} catch (Exception e) {
			System.out.println("Encountered an invalid link, skipping.");
			return;
		}

		// Get all links.
		Elements links = doc.select("a[href]");

		// Restrict the number of links traveled so as to encourage more
		// interesting-looking branching.
		if (links.size() > MAX_LINKS) {
			Elements chosenLinks = new Elements();
			List<Integer> seenIDs = new ArrayList<Integer>();

			// Randomly choose the links to follow.
			Random random = new Random();
			for (int i = 0; i < MAX_LINKS; ++i) {
				int index = random.nextInt(links.size());
				while (seenIDs.contains(index)) {
					index = random.nextInt(links.size());
				}
				chosenLinks.add(links.get(index));
			}

			links.clear();
			links.addAll(chosenLinks);
		}

		// Store URL data
		List<Document> grandChildren = new ArrayList<Document>();
		Map<Document, Integer> URLMap = new HashMap<Document, Integer>();
		boolean entry = true;

		// Use two loops to implement the BFS.
		while (!grandChildren.isEmpty() || entry) {
			entry = false;

			for (Element link : links) {
				// Sanitize the first child URL.
				String newURL = link.attr("abs:href");
				String sanNewURL = sanitizeURL(newURL);

				// Check to make sure there are no self-loops.
				if (sanNewURL.equals(sanURL)) {
				} else {
					int toID;

					// Check if the given URL is already in database
					query = "SELECT `RecordID` FROM record WHERE URL = '" + sanNewURL + "'";
					ResultSet rs = db.executeQuery(query);

					if (rs.next()) {
						// If it is, add the appropriate edge.
						toID = rs.getInt("RecordID");

						if (fromID != toID) {
							query = "INSERT INTO `web_crawler`.`edges` " + "(`from`, `to`) VALUES " + "(?, ?);";
							state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
							state.setString(1, fromID + "");
							state.setString(2, toID + "");
							state.execute();
						}
					} else {
						// If not, get page information.
						Document newDoc;
						try {
							newDoc = Jsoup.connect(newURL).get();

							// Store the new URL to prevent further processing.
							String newQuery = "INSERT INTO `web_crawler`.`record` " + "(`URL`) VALUES " + "(?);";
							PreparedStatement newState = db.connection.prepareStatement(newQuery,
									Statement.RETURN_GENERATED_KEYS);
							newState.setString(1, sanNewURL);
							newState.execute();
							grandChildren.add(newDoc);

							// Print the new site.
							System.out.println(sanNewURL);

							// Get the ID to which we add an edge.
							query = "SELECT `RecordID` FROM record WHERE URL = '" + sanNewURL + "'";
							ResultSet rsN = db.executeQuery(query);

							if (rsN.next()) {
								toID = rsN.getInt("RecordID");
							} else {
								toID = -1;
								System.out.println("Invalid ID!");
							}

							// Store this ID in the URL Map.
							URLMap.put(newDoc, toID);

							// If this is not a self-loop, add the edge.
							if (fromID != toID) {
								query = "INSERT INTO `web_crawler`.`edges` " + "(`from`, `to`) VALUES " + "(?, ?);";
								state = db.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
								state.setString(1, fromID + "");
								state.setString(2, toID + "");
								state.execute();
							}
						} catch (Exception e) {
							// This prevents Charlotte from choking on things
							// like PDFs, images, and non-parsable files.
							System.out.println("Encountered an invalid link, skipping: " + newURL);
						}
					}
				}
			}

			// All immediate children have been processed, find the next group.
			Document gChild = grandChildren.get(0);
			int gChildID = URLMap.get(gChild);
			fromID = gChildID;
			Elements gChildLinks = gChild.select("a[href]");

			// Restrict the number of links traveled so as to encourage more
			// interesting-looking branching.
			if (gChildLinks.size() > MAX_LINKS) {
				Elements gChildChosenLinks = new Elements();
				List<Integer> seenDockIDs = new ArrayList<Integer>();

				// Randomly choose the links to follow.
				Random random = new Random();
				for (int i = 0; i < MAX_LINKS; ++i) {
					int index = random.nextInt(gChildLinks.size());
					while (seenDockIDs.contains(index)) {
						index = random.nextInt(gChildLinks.size());
					}
					gChildChosenLinks.add(gChildLinks.get(index));
				}

				links.clear();
				links.addAll(gChildChosenLinks);
			} else {
				links.clear();
				links.addAll(gChildLinks);
			}

			// Repeat after removing the recently-processed child.
			grandChildren.remove(0);
		}
	}
}