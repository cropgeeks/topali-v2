#ifndef FILESHDR
#define FILESHDR

#include "runsettings.h"

#include <iostream>
#include <fstream>
#include <string>

class Files {
 public:
  Files(const RunSettings& rs) {
    openFile(rs,".lpd",flogp);
    openFile(rs,".par",fkappa);
    openFile(rs,".top",ftop);
    openFile(rs,".out",fout);
    openFile(rs,".tre",ftree);
    openFile(rs,".lst",flast);
  }
  ofstream flogp,fkappa,ftop,fout,ftree,flast;
 private:
  void openFile(const RunSettings&,const char*,ofstream&);
};

#endif
