#include "files.h"

void Files::openFile(const RunSettings& rs, const char *extension, ofstream& f)
{
  /* Opens a file for writing. */
  char filename[MAX_LINE];

  strcpy(filename,rs.getFileRoot()); 
  strcat(filename,extension);
  if((ifstream(filename)) != (void*)NULL) {
    error << "Error: You probably do not wish to overwrite " << filename 
	  << endError;
    quit(1);
  }
  f.open(filename);
  if(f.fail()) {
    error << "Error: Could not open " << filename << endError;
    quit(1);
  }
}

