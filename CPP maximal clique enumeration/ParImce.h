#ifndef SOURCE_PARIMCE_H_
#define SOURCE_PARIMCE_H_


#include "global.h"
#include "utils.h"
#include "undirectedgraph.h"


class ParImce {
	undirectedgraph _g_;	//the graph that goes through the changes
	undirectedgraph _h_;	//for storing the set of new edges
	tbb::atomic<long> new_clique_count;	//number of new maximal cliques
	tbb::atomic<long> subsumed_clique_count;	//number of subsumed cliques
	ConcurrentMap<string, bool> maxcliques; //the set of all maximal cliques, concurrent map for dealing with concurrency safe erase operation
	double computation_time_new;
	double computation_time_subsumed;
	void ParImceNew(vector<pair<int, int>> &);
	void CreateInducedSubgraph(unordered_set<int>&, undirectedgraph *);
	void ParEnumNew(undirectedgraph &, int, int, int, UMap<string, int> &);
	void ParExpand(undirectedgraph &, set<int> &, unordered_set<int> &, unordered_set<int> &, int, UMap<string, int> &);
	int ParFindPivot(const unordered_set<int> &, const unordered_set<int> &, undirectedgraph &);
	void ComputeSubsumedCliques(set<int> &);
	void computeNewCand(const unordered_set<int> &, const unordered_set<int> &, const unordered_map<int, int> &, int, unordered_set<int> *);
	void computeNewFini(const unordered_set<int> &, const unordered_set<int> &, const unordered_map<int, int> &, const vector<int> &, int, unordered_set<int> *);
	void ParComputeSubsumedCliques(set<int> &);
public:
	ParImce();
	ParImce(undirectedgraph &);
	void run(const string &, const string &, int, const string &);

	int getNewCliqueCount();
	int getSubCliqueCount();
	virtual ~ParImce();
};

#endif /* SOURCE_PARIMCE_H_ */