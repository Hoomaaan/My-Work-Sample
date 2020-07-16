// Tomita code for maximal clique enumaration

#include "tomita.h"
#include "utils.h"

tomita::tomita() {
	// TODO Auto-generated constructor stub
}

tomita::tomita(const undirectedgraph &g) {
	_g_ = g;
	cliquecount = 0;
}

void tomita::expand(vector<int> &k, unordered_set<int>& cand, unordered_set<int>& fini) {
	if (cand.empty() && fini.empty()) {
		cliquecount++;
		if (cliquecount % 10000000 == 0)
			cout << cliquecount << " maximal cliques generated\n";
		return;
	}
	if (cand.empty() && !fini.empty())
		return;
	int pivot = findPivot(cand, fini);
	auto cand_iterator = cand.begin();
	const auto cand_end_iterator = cand.end();
	const auto neighbor_of_pivot_end_iterator = _g_.neighbor(pivot).end();
	while (cand_iterator != cand_end_iterator) {
		if (_g_.neighbor(pivot).find(*cand_iterator) == neighbor_of_pivot_end_iterator) {
			k.push_back(*cand_iterator);
			unordered_set<int> cand_q;
			utils::unordered_intersect(cand,_g_.neighbor(*cand_iterator), &cand_q);
			unordered_set<int> fini_q;
			utils::unordered_intersect(fini,_g_.neighbor(*cand_iterator), &fini_q);
			expand(k, cand_q, fini_q);
			fini.insert(*cand_iterator);
			cand_iterator = cand.erase(cand_iterator);
			k.pop_back();
		} else {
			cand_iterator++;
		}
	}
}

int tomita::findPivot(const unordered_set<int> &cand,
		const unordered_set<int> &fini) {
	int size = -1;
	int p = -1;
	for (int u : cand) {
		if((int)_g_.neighbor(u).size() < size)
			continue;	
		int size_of_q = utils::unordered_intersect_size(_g_.neighbor(u), cand);
		if (size < size_of_q) {
			size = size_of_q;
			p = u;
		}
	}

	for (int u : fini) {
		if((int)_g_.neighbor(u).size() < size)
			continue;	
		int size_of_q = utils::unordered_intersect_size(_g_.neighbor(u), cand);
		if (size < size_of_q) {
			size = size_of_q;
			p = u;
		}
	}
	return p;
}

void tomita::run() {
	vector<int> k;
	unordered_set<int> fini;
	unordered_set<int> cand = _g_.V();
	cout << "calling expand\n";
	int cand_size = cand.size();
	int fini_size = fini.size();
	expand(k, cand, fini);
}

int tomita::numberOfCliques() {
	return cliquecount;
}

tomita::~tomita() {
	// TODO Auto-generated destructor stub
}