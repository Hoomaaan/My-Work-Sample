// This code count maximal cliques in a graph in parallel setting

#include "ParImce.h"

ParImce::ParImce() {
	// TODO Auto-generated constructor stub

	computation_time_new = 0;
	computation_time_subsumed = 0;
}

ParImce::ParImce(undirectedgraph& g) {
	_g_ = g;
	computation_time_new = 0;
	computation_time_subsumed = 0;
}

void ParImce::ParImceNew(vector<pair<int, int>>& batch) {
	new_clique_count = 0;
	subsumed_clique_count = 0;
	computation_time_new = 0;
	computation_time_subsumed = 0;
	undirectedgraph h;
	_h_ = h;
	UMap<string, int> RT;
	int index = 0;

	for (auto e : batch) {
		int u = e.first;
		int v = e.second;
		_g_.addEdge(u, v);
		_h_.addEdge(u, v);
		index++;
		string edge = to_string(u) + " " + to_string(v);
		RT[edge] = index;
	}

	tbb::parallel_for_each(batch.begin(), batch.end(),
			[&](pair<int,int> e) {
				int u = e.first;
				int v = e.second;
				string edge = to_string(u) + " " + to_string(v);
				int index = RT[edge];
				unordered_set<int> common_neighborhood;
				utils::unordered_intersect(_g_.neighbor(u), _g_.neighbor(v), &common_neighborhood);
				common_neighborhood.insert(u);
				common_neighborhood.insert(v);
				undirectedgraph g_e;
				CreateInducedSubgraph(common_neighborhood, &g_e);
				ParEnumNew(g_e, u, v, index, RT);
			});
}

void ParImce::ParEnumNew(undirectedgraph &g, int u, int v, int index,
		UMap<string, int> &RT) {

	set<int> k;
	unordered_set<int> fini;
	unordered_set<int> cand = g.V();
	ParExpand(g, k, cand, fini, index, RT);

}

void ParImce::ParExpand(undirectedgraph& g, set<int>& k,
		unordered_set<int>& cand, unordered_set<int>& fini, int index,
		UMap<string, int>& RT) {
	if (cand.empty() && fini.empty()) {
		new_clique_count.fetch_and_increment();	//atomic increment
		if (new_clique_count % 100000000 == 0)
			cout << new_clique_count << " maximal cliques generated\n"
		string result;
		for (int u : k) {
			result += std::to_string(u) + " ";
		}
		ConcurrentMap<string, bool>::accessor ac;
		maxcliques.insert(ac, result);
		ac->second = true;
		ParComputeSubsumedCliques(k);
		return;
	}
	if (cand.empty() && !fini.empty())
		return;
	
	int pivot;
	pivot = ParFindPivot(cand, fini, g);
	vector<int> ext;
	const auto neighbor_of_pivot_end_iterator = g.neighbor(pivot).end();
	std::unordered_map<int, int> exttoindex;
	int id = 0;
	for (int w : cand) {
		if (g.neighbor(pivot).find(w) == neighbor_of_pivot_end_iterator) {
			ext.push_back(w);
			exttoindex.insert( { w, id });
			id++;
		}
	}

	tbb::parallel_for(tbb::blocked_range<int>(0, ext.size()),
			[&](tbb::blocked_range<int> r) {
				for(int idx = r.begin(); idx < r.end(); ++idx) {
					int q = ext[idx];

					set<int> k_q = k;

					k_q.insert(q);

					bool flag = false;

					for(auto v : k) {
						if(_h_.ContainsVertex(q) && (_h_.neighbor(q).find(v) != _h_.neighbor(q).end())) {
							if(v < q) {
								string edge = to_string(v) + " " + to_string(q);
								if(RT[edge] < index) {
									return;
								}
							} else {
								string edge = to_string(q) + " " + to_string(v);
								if(RT[edge] < index) {
									return;
								}
							}
						}
					}

					unordered_set<int> cand_q;
					unordered_set<int> fini_q;

					computeNewCand(cand, g.neighbor(q), exttoindex, idx, &cand_q);
					computeNewFini(fini, g.neighbor(q), exttoindex, ext, idx, &fini_q);

					//if(cand_q.size() > 50)
					ParExpand(g, k_q, cand_q, fini_q, index, RT);
					//else
					//seq_expand(k_q, cand_q, fini_q);

				}
			});
}

void ParImce::ParComputeSubsumedCliques(set<int>& k) {
	tbb::concurrent_vector<set<int>> S[2];
	S[0].push_back(k);
	int idx = 0;
	for (int u : k) {
		for (int v : k) {
			if ((u < v) && (_h_.ContainsVertex(u) && _h_.ContainsVertex(v)) &&  _h_.ContainsEdge(u, v)) {
				idx = 1 - idx;
				tbb::parallel_for_each(S[1-idx].begin(), S[1-idx].end(), [&](set<int> t){
					if ((t.count(u) > 0) && (t.count(v) > 0)) {
						set<int> x(t);
						set<int> y(t);

						x.erase(u);
						y.erase(v);

						S[idx].push_back(x);
						S[idx].push_back(y);
					} else {
						S[idx].push_back(t);
					}
				});
				S[1 - idx].clear();
			}
		}
	}

	tbb::parallel_for_each(S[idx].begin(), S[idx].end(), [&](set<int> c) {
		string result;
		for(int u : c) {
			result += std::to_string(u) + " ";
		}
		if(maxcliques.erase(result) /*returns true if the erase is successful. only one thread can erase*/) {
			subsumed_clique_count.fetch_and_increment();
		}
	});
}

int ParImce::ParFindPivot(const unordered_set<int>& cand,
		const unordered_set<int>& fini, undirectedgraph& g) {
	ConcurrentMap<int, int> v_to_intersect_size;
	tbb::parallel_for_each(cand.begin(), cand.end(),
			[&](int u) {

				int intersect_size = utils::unordered_intersect_size(_g_.neighbor(u), cand);

				tbb::concurrent_hash_map<int, int>::accessor ac;

				v_to_intersect_size.insert(ac, u);/*inserted as (value, key) pair in concurrent_hash_map*/

				ac->second = intersect_size;
			});

	tbb::parallel_for_each(fini.begin(), fini.end(),
			[&](int u) {

				int intersect_size = utils::unordered_intersect_size(_g_.neighbor(u), cand);

				ConcurrentMap<int, int>::accessor ac;

				v_to_intersect_size.insert(ac, u);

				ac->second = intersect_size;
			});

	int size = -1;
	int v = 0;

	for (ConcurrentMap<int, int>::iterator i = v_to_intersect_size.begin();
			i != v_to_intersect_size.end(); i++) {
		int u = i->first;
		int tmp_size = i->second;

		if (size < tmp_size) {
			size = tmp_size;
			v = u;
		}
	}

	return v;
}

void ParImce::computeNewCand(const unordered_set<int> &cand,
		const unordered_set<int> &nghofq, const unordered_map<int, int> &ext,
		int idx, unordered_set<int> *result) {

	if (cand.size() > nghofq.size()) {
		for (int w : nghofq) {
			if (cand.find(w) != cand.end()) {
				if ((ext.find(w) != ext.end()) && (ext.at(w) > idx))
					(*result).insert(w);
				if (ext.find(w) == ext.end())
					(*result).insert(w);
			}
		}
	} else {
		for (int w : cand) {
			if (nghofq.find(w) != nghofq.end()) {
				if ((ext.find(w) != ext.end()) && (ext.at(w) > idx))
					(*result).insert(w);
				if (ext.find(w) == ext.end())
					(*result).insert(w);
			}
		}
	}
}

void ParImce::computeNewFini(const unordered_set<int> &fini,
		const unordered_set<int> &nghofq, const unordered_map<int, int> &ext,
		const vector<int>& extvec, int idx, unordered_set<int> *result) {

	if (nghofq.size() < fini.size() + idx) {
		for (int w : nghofq) {
			if (fini.find(w) != fini.end()) {
				(*result).insert(w);
			}
			if ((ext.find(w) != ext.end()) && (ext.at(w) < idx))
				(*result).insert(w);
		}
	} else {
		for (int w : fini) {
			if (nghofq.find(w) != nghofq.end())
				(*result).insert(w);
		}
		for (int i = 0; i < idx; i++) {
			int x = extvec[i];
			if (nghofq.find(x) != nghofq.end())
				(*result).insert(x);
		}
	}
}

void ParImce::run(const string &clique_file, const string &edge_file,
		int batch_size, const string &out_fname) {

	std::ifstream edgestream(edge_file.c_str());

	std::ifstream cliquestream(clique_file.c_str());

	int index = 0;
	int count = 0;

	string line;

	while (!cliquestream.eof()) {

		count++;

		std::getline(cliquestream, line);

		stringstream strm(line);

		int u;

		set<int> clique;

		while (strm >> u) {
			clique.insert(u);
		}

		string result;

		for (int u : clique) {
			result += std::to_string(u) + " ";
		}

		if (!result.empty()) {
			ConcurrentMap<string, bool>::accessor ac;
			maxcliques.insert(ac, result);
			ac->second = true;
		}

	}

	cout << "Reading initial maximal cliques done\n";

	cout << "number of maximal clique of the initial graph: "
			<< maxcliques.size() << "\t" << count << "\n";

	ofstream out_file;

	out_file.open(out_fname.c_str(), ios::app);

	out_file << "Results of ParIMCE on batch size: " << batch_size << "\n";

	out_file
			<< "iteration\tnumber_new\tnumber_subsumed\ttime_new(ms)\ttiem_subsumed(ms)\n";

	out_file.close();

	count = 0;

	while (true) {

		index = 0;
		count++;

		vector<pair<int, int>> batch_of_edges;

		while (index < batch_size) {
			if (edgestream.good() && !edgestream.eof()) {

				std::getline(edgestream, line);
				stringstream strm(line);
				if (!line.empty() && strm.good() && !strm.eof()) {
					int u;
					int v;
					strm >> u;
					strm >> v;

					if (u < v)
						batch_of_edges.push_back(make_pair(u, v));
					else
						batch_of_edges.push_back(make_pair(v, u));
				}
				index++;
			} else {
				break;
			}
		}

		auto start = std::chrono::high_resolution_clock::now();

		ParImceNew(batch_of_edges);

		auto end = std::chrono::high_resolution_clock::now();

		auto elapsed =
				std::chrono::duration<double, std::milli>(end - start).count();

		computation_time_new = elapsed - computation_time_subsumed;

		out_file.open(out_fname.c_str(), ios::app);

		out_file << count << "\t" << new_clique_count << "\t"
				<< subsumed_clique_count << "\t" << computation_time_new << "\t"
				<< computation_time_subsumed << "\n";

		cout << count << "\t" << new_clique_count << "\t"
				<< subsumed_clique_count << "\t" << maxcliques.size() << "\n";

		out_file.close();

		if (count == 1000)
			break;
	}
}

void ParImce::CreateInducedSubgraph(unordered_set<int>& vertices,
		undirectedgraph *g) {

	for (int u : vertices) {
		for (int v : vertices) {
			if (_g_.neighbor(u).find(v) != _g_.neighbor(u).end()) {
				(*g).addEdge(u, v);
			}
		}
	}
}

ParImce::~ParImce() {
	// TODO Auto-generated destructor stub
}