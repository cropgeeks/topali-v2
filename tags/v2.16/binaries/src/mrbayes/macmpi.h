/* header file for MacMPI.c   */

#define MPI_COMM_WORLD     0
#define MPI_SUCCESS        0
#define MPI_BYTE           2
#define MPI_INT           18
#define MPI_LONG          18
#define MPI_FLOAT         19
#define MPI_DOUBLE        20
#define MPI_FLOAT_INT     29
#define MPI_DOUBLE_INT    30
#define MPI_2INT          37
#define MPI_UNDEFINED     -1
#define MPI_ERR_IN_STATUS 67
#define MPI_ANY_SOURCE    -1
#define MPI_ANY_TAG       -1
#define MPI_REQUEST_NULL  -1
#define MPI_COMM_SELF      1
#define MPI_COMM_NULL     -1
#define MPI_PROC_NULL     -3

enum { MPI_MAX, MPI_MIN, MPI_SUM, MPI_MAXLOC=4,MPI_MINLOC=5 };

#define MPI_CHAR          MPI_BYTE

#define MPI_SOURCE source
#define MPI_TAG tag
#define MPI_ERROR error

typedef struct {
   int source;
   int tag;
   int error;
   int len;
   int type;
} MPI_Status;

typedef int MPI_Comm;
typedef int MPI_Datatype;
typedef int MPI_Request;
typedef int MPI_Op;
typedef int MPI_Aint;

int MPI_Init(int *argc, char ***argv);
int MPI_Finalize(void);
int MPI_Send(void* buf, int count, MPI_Datatype datatype, int dest,
             int tag, MPI_Comm comm);
int MPI_Recv(void* buf, int count, MPI_Datatype datatype, int source,
             int tag, MPI_Comm comm, MPI_Status *status);
int MPI_Isend(void* buf, int count, MPI_Datatype datatype, int dest,
              int tag, MPI_Comm comm, MPI_Request *request);
int MPI_Irecv(void* buf, int count, MPI_Datatype datatype, int source,
              int tag, MPI_Comm comm, MPI_Request *request);
int MPI_Test(MPI_Request *request, int *flag, MPI_Status *status);
int MPI_Wait(MPI_Request *request, MPI_Status *status);
int MPI_Sendrecv(void* sendbuf, int sendcount, MPI_Datatype sendtype,
                  int dest, int sendtag, void* recvbuf, int recvcount,
                  MPI_Datatype recvtype, int source, int recvtag,
                  MPI_Comm comm, MPI_Status *status);
int MPI_Ssend(void* buf, int count, MPI_Datatype datatype, int dest,
              int tag, MPI_Comm comm);
int MPI_Issend(void* buf, int count, MPI_Datatype datatype, int dest,
               int tag, MPI_Comm comm, MPI_Request *request);
int MPI_Waitall(int count, MPI_Request *array_of_requests,
                MPI_Status *array_of_statuses);
int MPI_Waitany(int count, MPI_Request *array_of_requests,
                int *index, MPI_Status *status);
int MPI_Get_count(MPI_Status *status, MPI_Datatype datatype,
                  int *count);
int MPI_Initialized(int *flag);
int MPI_Comm_size(MPI_Comm comm, int *size);
int MPI_Comm_rank(MPI_Comm comm, int *rank);
int MPI_Comm_dup(MPI_Comm comm, MPI_Comm *newcomm);
int MPI_Comm_split(MPI_Comm comm, int color, int key, MPI_Comm *newcomm);
int MPI_Comm_free(MPI_Comm *comm);
int MPI_Cart_create(MPI_Comm comm_old, int ndims, int *dims, int *periods,
                    int reorder, MPI_Comm *comm_cart);
int MPI_Cart_coords(MPI_Comm comm, int rank, int maxdims, int *coords);
int MPI_Cart_get(MPI_Comm comm, int maxdims, int *dims, int *periods,
                int *coords);
int MPI_Cart_shift(MPI_Comm comm, int direction, int disp, int *rank_source,
                   int *rank_dest);
int MPI_Cart_rank(MPI_Comm comm, int *coords, int *rank);
int MPI_Cart_sub(MPI_Comm comm, int *remain_dims, MPI_Comm *newcomm);
int MPI_Dims_create(int nnodes, int ndims, int *dims);
int MPI_Bcast(void* buffer, int count, MPI_Datatype datatype,
              int root, MPI_Comm comm);
int MPI_Barrier(MPI_Comm comm);
int MPI_Reduce(void* sendbuf, void* recvbuf, int count,
               MPI_Datatype datatype, MPI_Op op, int root,
               MPI_Comm comm);
int MPI_Scan(void* sendbuf, void* recvbuf, int count,
             MPI_Datatype datatype, MPI_Op op, MPI_Comm comm);
int MPI_Allreduce(void* sendbuf, void* recvbuf, int count,
                  MPI_Datatype datatype, MPI_Op op, MPI_Comm comm);
int MPI_Gather(void* sendbuf, int sendcount, MPI_Datatype sendtype,
               void* recvbuf, int recvcount, MPI_Datatype recvtype,
               int root, MPI_Comm comm);
int MPI_Allgather(void* sendbuf, int sendcount,
                  MPI_Datatype sendtype, void* recvbuf, int recvcount,
                  MPI_Datatype recvtype, MPI_Comm comm);
int MPI_Scatter(void* sendbuf, int sendcount, MPI_Datatype sendtype,
                void* recvbuf, int recvcount, MPI_Datatype recvtype,
                int root, MPI_Comm comm);
int MPI_Alltoall(void* sendbuf, int sendcount, MPI_Datatype sendtype,
                 void* recvbuf, int recvcount, MPI_Datatype recvtype,
                 MPI_Comm comm);
int MPI_Gatherv(void* sendbuf, int sendcount, MPI_Datatype sendtype,
                void* recvbuf, int *recvcounts, int *displs,
                MPI_Datatype recvtype, int root, MPI_Comm comm);
int MPI_Allgatherv(void* sendbuf, int sendcount,
                   MPI_Datatype sendtype, void* recvbuf, int *recvcounts,
                   int *displs, MPI_Datatype recvtype, MPI_Comm comm);
int MPI_Scatterv(void* sendbuf, int *sendcounts, int *displs,
                 MPI_Datatype sendtype, void* recvbuf, int recvcount,
                 MPI_Datatype recvtype, int root, MPI_Comm comm);
int MPI_Alltoallv(void* sendbuf, int *sendcounts, int *sdispls,
                  MPI_Datatype sendtype, void* recvbuf, int *recvcounts,
                  int *rdispls, MPI_Datatype recvtype, MPI_Comm comm);
int MPI_Reduce_scatter(void* sendbuf, void* recvbuf, int *recvcounts,
                  MPI_Datatype datatype, MPI_Op op, MPI_Comm comm);
int MPI_Abort(MPI_Comm comm, int errorcode);
double MPI_Wtime(void);
double MPI_Wtick(void);
int MPI_Type_extent(MPI_Datatype datatype, MPI_Aint *extent);
void Logname(char* name);
void Set_Mon(int monval);