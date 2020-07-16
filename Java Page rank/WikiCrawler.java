import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiCrawler {
	public String seedUrl;
	public String[] keywords;
	public int max;
	public String fileName;
	public boolean isTopicSensitive;
	public static final String BASE_URL = "https://en.wikipedia.org";
	public static final String ROBOT = "robots.txt";
	public static final int waitingTime = 2;
	private static final int d = 17;
	private HashMap<String, Integer> mappedLinks;
	private HashMap<Integer, String> mappedLinksReverse;
	private HashSet<Integer> notAllowed;
	private int linkNumber;
	private int[] vertices;
	private ArrayList<Integer>[] adjacency;
	private HashSet<Integer> crawledVertices;

	// constructor
	public WikiCrawler(String seedUrl, String[] keywords, int max, String fileName, boolean isTopicSensitive) {
		this.seedUrl = seedUrl;
		this.keywords = keywords;
		for (int i = 0; i < keywords.length; i++) {
			keywords[i] = keywords[i].toLowerCase();
		}
		this.fileName = fileName;
		this.max = max;
		this.isTopicSensitive = isTopicSensitive;
		mappedLinks = new HashMap<String, Integer>();
		mappedLinksReverse = new HashMap<Integer, String>();
		notAllowed = new HashSet<Integer>();
		linkNumber = 0;
		notAllowedLinks(ROBOT);
	}

	// method
	public void crawl() {
		addLink(seedUrl);
		int seedUrlNumber = mappedLinks.get(seedUrl);
		vertices = new int[max];
		adjacency = new ArrayList[max];
		PriorityQueue<Pair<Double, Integer>> Q = new PriorityQueue<Pair<Double, Integer>>();
		int requests = 0;
		Q.add(new Pair(0, seedUrlNumber));
		HashSet<Integer> visited = new HashSet<Integer>();
		visited.add(seedUrlNumber);
		int count = 0, ones = 0;
		while (count < max && !Q.isEmpty()) {
			Pair<Double, Integer> currentPair = Q.poll();
			System.out.println(mappedLinksReverse.get(currentPair.e2));
			Integer pageAddress = currentPair.e2;
			if (pageAddress < notAllowed.size())
				continue;
			vertices[count] = pageAddress;
			// Politeness
			if (requests > 0 && requests % 10 == 0) {
				try {
					System.err.println("You have made 10 requests, please wait for 2 seconds then continue :)");
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			requests++;
			ArrayList<Pair<Double, Integer>> neighbors = retrieveNeighbors(pageAddress);
			if (neighbors.size() == 0)
				continue;
			adjacency[count] = new ArrayList<Integer>();
			for (Pair<Double, Integer> p : neighbors) {
				if (p.e2.equals(pageAddress))
					continue;
				adjacency[count].add(p.e2);
				if (visited.contains(p.e2) == false) {
					if ((isTopicSensitive == false && Q.size() >= max) == false
							&& (isTopicSensitive == true && ones >= max) == false) {
						if (p.e1 == 1.0)
							ones++;
						Q.add(new Pair(p.e1, p.e2));
					}
					visited.add(p.e2);
				}
			}
			count++;
		}
		crawledVertices = new HashSet<Integer>();
		for (int v : vertices)
			crawledVertices.add(v);
		writeInFile();
		return;
	}

	private void notAllowedLinks(String links) {
		String webLink = BASE_URL + "/" + links;
		URL url = null;
		try {
			url = new URL(webLink);
		} catch (MalformedURLException e) {
			return;
		}
		InputStream is = null;
		try {
			is = url.openStream();
		} catch (IOException e) {
			return;
		}
		StringBuilder html = new StringBuilder("");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				Pattern pagePattern = Pattern.compile("[\\n\\r]*Disallow:\\s*\\/([^\\n\\r#].*)",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
				Matcher pageMatcher = pagePattern.matcher(line);
				while (pageMatcher.find()) {
					String link = pageMatcher.group(1);
					addLink(link);
					notAllowed.add(mappedLinks.get(link));
				}
			}
		} catch (IOException e) {
			return;
		}
		return;
	}

	private void addLink(String link) {
		if (mappedLinks.containsKey(link))
			return;
		mappedLinks.put(link, linkNumber);
		mappedLinksReverse.put(linkNumber, link);
		linkNumber++;
		return;
	}

	private class Pair<T1, T2> implements Comparable<Pair<T1, T2>> {
		final T1 e1;
		final T2 e2;
		final boolean e1Comparable;
		final boolean e2Comparable;

		Pair(final T1 e1, T2 e2) {
			this.e1 = e1;
			this.e2 = e2;
			this.e1Comparable = e1 instanceof Comparable;
			this.e2Comparable = e2 instanceof Comparable;
		}

		@Override
		public int compareTo(Pair<T1, T2> o) {
			if (e1Comparable) {
				final int k = ((Comparable<T1>) e1).compareTo(o.e1);
				if (k < 0)
					return 1;
				if (k > 0)
					return -1;
			}
			if (e2Comparable) {
				final int k = ((Comparable<T2>) e2).compareTo(o.e2);
				if (k < 0)
					return 1;
				if (k > 0)
					return -1;
			}
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Pair) {
				final Pair<T1, T2> o = (Pair<T1, T2>) obj;
				return (e1.equals(o.e1) && e2.equals(o.e2));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 19 * hash + (e1 != null ? e1.hashCode() : 0);
			hash = 19 * hash + (e2 != null ? e2.hashCode() : 0);
			return hash;
		}

	}

	private ArrayList<Pair<Double, Integer>> retrieveNeighbors(Integer pageAddressNumber) {
		String pageAddress = mappedLinksReverse.get(pageAddressNumber);
		URL url = null;
		try {
			url = new URL(
					BASE_URL + (pageAddress.length() > 0 && pageAddress.charAt(0) == '/' ? "" : "/") + pageAddress);
		} catch (MalformedURLException e) {
			return new ArrayList<Pair<Double, Integer>>();
		}
		InputStream is = null;
		try {
			is = url.openStream();
		} catch (IOException e) {
			return new ArrayList<Pair<Double, Integer>>();
		}

		StringBuilder html = new StringBuilder("");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		ArrayList<Pair<Double, Integer>> neighbors = new ArrayList<Pair<Double, Integer>>();
		try {
			boolean go = false;
			while ((line = br.readLine()) != null) {
				go |= line.contains("<p>");
				if (go) {
					String page = line;
					Pattern pieceOfPagePattern = Pattern.compile(
							"<a\\s*(?i)href\\s*=\"\\s*(\"([^\"]*\")|('[^']*')|([^'\">\\s]+))(.+?)</a>",
							Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
					Matcher pieceOfPageMatcher = pieceOfPagePattern.matcher(page);
					while (pieceOfPageMatcher.find()) {
						String link;
						if (isValidLink((link = pieceOfPageMatcher.group(1)))) {
							int start = pieceOfPageMatcher.start();
							int end = pieceOfPageMatcher.end();
							int endAnchorText = end - 4;
							int startAnchorText = endAnchorText;
							while (page.charAt(startAnchorText) != '>')
								startAnchorText--;
							String anchor = page.substring(startAnchorText, endAnchorText).toLowerCase();
							addLink(link);
							Double weight = weightCalculator(link, anchor, start, end, page);
							int numLink = mappedLinks.get(link);
							neighbors.add(new Pair(weight, numLink));
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return neighbors;
	}

	private boolean isValidLink(String link) {
		if (link.indexOf(':') != -1 || link.indexOf('#') != -1)
			return false;
		return true;//!(link.startsWith("/w/index.php") || link.startsWith("/wiki/index.php"))
				//&& (link.startsWith("/w/") || link.startsWith("/wiki/"));
	}

	private double weightCalculator(String link, String anchor, int start, int end, String page) {
		if (isTopicSensitive == false)
			return 0.0;
		link = link.toLowerCase();
		for (String keyword : keywords) {
			if (link.contains(keyword) || anchor.contains(keyword))
				return 1.0;
		}
		int prefix = getNearDwords(page, start, true, d);
		int suffix = getNearDwords(page, end, false, prefix);
		int nearest = Math.min(prefix, suffix);
		if (nearest > d)
			return 0.0;
		return 1.0 / (nearest + 2.0);
	}

	private int getNearDwords(String str, int start, boolean state, int max) {
		if (str.length() == 0) {
			return d + 1;
		}
		int space = 0;
		boolean flag = true;
		StringBuilder strBuilder = new StringBuilder("");
		for (int x = start + (state == true ? -1 : 1); x >= -1 && x <= str.length(); x += (state == true ? -1 : +1)) {
			if (x == str.length() || x == -1 || (str.charAt(x) == ' ' && flag == false)) {
				space++;
				if (state == true)
					strBuilder = strBuilder.reverse();
				String s = strBuilder.toString();
				for (String key : keywords) {
					if (s.contains(key))
						return space;
				}
				if (space >= max)
					break;
				flag = true;
				strBuilder = new StringBuilder("");
			} else if (str.charAt(x) != ' ') {
				strBuilder = strBuilder.append(str.charAt(x));
				flag = false;
			}
		}
		return d + 1;
	}

	private void writeInFile() {
		File file = new File(fileName);
		FileWriter fr = null;
		try {
			fr = new FileWriter(file);
			fr.write(max + "\n");
			for (int i = 0; i < vertices.length; i++) {
				if (adjacency [i] == null)
					break ;
				int a = vertices[i];
				HashSet<Integer> distinct = new HashSet<Integer>();
				for (int b : adjacency[i]) {
					if (crawledVertices.contains(b) == false)
						continue;
					if (distinct.contains(b))
						continue;
					distinct.add(b);
					fr.write(mappedLinksReverse.get(a) + " " + mappedLinksReverse.get(b) + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
