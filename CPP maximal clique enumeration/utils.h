#ifndef SOURCE_UTILS_H_
#define SOURCE_UTILS_H_

#include<unordered_set>
using namespace std;

class utils {

	static int size;

public:

	static void unordered_intersect(const unordered_set<int> &A,
			const unordered_set<int> &B, unordered_set<int> *result) {


		if (A.size() > B.size()) {
			for (int i : B) {
				if (A.find(i) != A.end()){

					(*result).insert(i);
				}
			}
		} else {
			for (int i : A) {
				if (B.find(i) != B.end()){
					(*result).insert(i);
				}
			}
		}

		//return *result;
	}

	static int intersection_size(){
		return size;
	}

	static int unordered_intersect_size(const unordered_set<int> &A,
			const unordered_set<int> &B) {

		int count=0;

		if (A.size() > B.size()) {
			for (int i : B) {
				if (A.find(i) != A.end())
					count++;
			}
		} else {
			for (int i : A) {
				if (B.find(i) != B.end())
					count++;
			}
		}

		return count;
	}
};

#endif /* SOURCE_UTILS_H_ */