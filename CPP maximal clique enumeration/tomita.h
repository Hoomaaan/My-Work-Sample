#ifndef ALGORITHM_TOMITA_H_
#define ALGORITHM_TOMITA_H_

#include <set>
#include <vector>
#include <unordered_set>
#include "undirectedgraph.h"
using namespace std;

class tomita {
	undirectedgraph _g_;
	long cliquecount;
	void expand(vector<int> &k, unordered_set<int> &cand, unordered_set<int> &fini);
	int findPivot(const unordered_set<int> &A, const unordered_set<int> &B);
public:
	tomita();
	tomita(const undirectedgraph &g);
	int numberOfCliques();
	void run();

	virtual ~tomita();
};

#endif /* ALGORITHM_TOMITA_H_ */