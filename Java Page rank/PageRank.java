import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class PageRank {
	private int N;
	private int E;
	private int[] inDeg;
	private int[] outDeg;
	private ArrayList<Integer>[] neighbor;
	private Pair <Integer, Integer> [] topInDeg;
	private Pair <Integer, Integer> [] topOutDeg;
	private double[] PG;
	private Pair<Double, Integer>[] topPG;
	
	public PageRank (String fileName, double epsilon, double beta) {
		GraphConstructor(fileName);
		double[] current = new double[N];
		double[] next;
		Arrays.fill(current, 1.0/N);
		int iteration = 0;
		for (iteration = 0; norm(next = A(current, beta), current) > epsilon; iteration ++, current = next);
		PG = current;
		topPG = new Pair[N] ;
		for(int i = 0; i < N; i++) {
			topPG[i] = new Pair<Double, Integer> (PG[i], i);
		}
		Arrays.sort(topPG, new Comparator<Pair<Double, Integer>>() {
			public int compare(Pair<Double, Integer> o1, Pair<Double, Integer> o2) {
				return o2.e1.compareTo(o1.e1);
			}
		});
		System.out.println("Number of Iterations: " + iteration);
	}
	
	public double pageRankOf(int link) {
		if (link - 1 > N || link -1 < 0) {
			System.err.println("Page Not Found");
			return 0.0;
		}
		return PG [link - 1];
	}
	public int outDegreeOf(int link) {
		if (link - 1 > N || link -1 < 0) {
			System.err.println("Page Not Found");
			return 0;
		}
		return outDeg[link - 1];
	}
	public int inDegreeOf(int link) {
		if (link - 1 > N || link -1 < 0) {
			System.err.println("Page Not Found");
			return 0;
		}
		return inDeg[link - 1];
	}
	public int numEdges() {
		return this.E;
	}
	public int[] topKPageRank(int k) {
		k = Math.min(k, topPG.length);
		int[] res = new int[k];
		for (int i = 0; i < k ; i++) {
			res[i] = topPG[i].e2 + 1;
		}
		return res;
	}
	public int[] topKInDegree(int k) {
		k = Math.min(k, topInDeg.length);
		int[] res = new int[k];
		for (int i = 0; i < k; i++) {
			res[i] = topInDeg[i].e2 + 1;
		}
		return res;
	}
	public int[] topKOutDegree(int k) {
		k = Math.min(k, topInDeg.length);
		int[] res = new int[k];
		for (int i = 0; i < k; i++) {
			res[i] = topOutDeg[i].e2 + 1;
		}
		return res;
	}
	
	private void GraphConstructor (String fileName) {
		try {
			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			{
				this.N= Integer.parseInt(line);
				neighbor = new ArrayList[N];
				for(int i = 0; i < N; i++) {
					neighbor[i] = new ArrayList();
				}
				outDeg = new int[N];
				Arrays.fill(outDeg, 0);
				inDeg = new int[N];
				Arrays.fill(inDeg, 0);
				this.E = 0;
			}
			while ((line = br.readLine()) != null) {
				String[] edge = line.split("\\s+");
				{	
					int a = Integer.parseInt(edge[0]) - 1;
					int b = Integer.parseInt(edge[1]) - 1;
					neighbor[a].add(b);
					outDeg[a]++;
					inDeg[b]++;
					E++;
				}
			}
			br.close();
		}catch (FileNotFoundException e) {
			System.out.println("Could not open " + fileName);
		}catch(IOException e) {
			System.out.println("Error in Reading " + fileName);
		}
		topInDeg = new Pair[N];
		topOutDeg = new Pair[N];
		for (int i = 0; i < N; i++) {
			topInDeg[i] = new Pair (inDeg[i], i);
			topOutDeg[i] = new Pair(outDeg[i], i);
		}
		Arrays.sort(topInDeg, new Comparator<Pair<Integer, Integer>>(){
			public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
				return o2.e1.compareTo(o1.e1);
			}
		});
		Arrays.sort(topOutDeg, new Comparator<Pair <Integer, Integer>>(){
			public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
				return o2.e1.compareTo(o1.e1);
			}
		});
		return;
	}
	
	private double norm (double[] nxt, double[] cur) {
		double res = 0;
		for (int i = 0; i < N; i++) {
			res += Math.abs(nxt[i] - cur[i]);
		}
		return res;
	}
	
	private class Pair<S,T> implements Comparable<Pair<S,T>> {
        final S e1;
        final T e2;
        final boolean e1Comparable;
        final boolean e2Comparable;

        Pair(final S e1, final T e2) {
            this.e1 = e1;
            this.e2 = e2;

            this.e1Comparable = (e1 instanceof Comparable);
            this.e2Comparable = (e2 instanceof Comparable);
        }

        public int compareTo(Pair<S, T> o) {
            if (e1Comparable) {
                final int k = ((Comparable<S>) e1).compareTo(o.e1);
                if (k < 0) return 1;
                if (k > 0) return -1;
            }
            if (e2Comparable) {
                final int k = ((Comparable<T>) e2).compareTo(o.e2);
                if (k < 0) return 1;
                if (k > 0) return -1;
            }
            return 0;
        }
        @Override
        public boolean equals(
                Object obj) {
            if (obj instanceof Pair) {
                final Pair<S, T> o = (Pair<S, T>) obj;
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
	
	private double[] A(double[] cur, double beta) {
		double [] nxt = new double [N];
		Arrays.fill(nxt,  (1.0 - beta) / N);
		double add = 0;
		for (int i = 0; i < N; i++) {
			if (outDeg[i] != 0) {
				for (int j : neighbor[i]) {
					nxt[j] += beta * cur[i] / outDeg[i]; 
				}
			}else {
				add += beta * cur[i] / N;
			}
		}
		for (int i = 0; i < N; i++)
			nxt[i] += add;
		return nxt;
	}
}
