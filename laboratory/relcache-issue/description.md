#### Problem:
Very rare exceptions linked to race condition between instances. Check file exception.stacktrace.

When instance 1 is trying to save cache element to DB, instance 2 already did it, so Hibernate shoots JpaSystemException.

#### Reproduction:
1) Run local postgres with data;
2) Run 10 instances of application. Easiest way is on screenshot:
![config](idea-run-config.png)
3) Run in separate project GrpcCallCheck.java. Specify USER from authz.rel_cache. 
4) Check application logs for JpaSystemException. If there are no exceptions, try to increase REQUESTS_COUNT in 
   GrpcCallCheck.

#### Solution:
Increase variety of primary ID. 

Currently, primary ID is a composite key, which is assembled from user, tag and path. Now revision is added to this key.

There is a chance that variety doesn't matter here, in such case solution must be refactored for optimistic lock. 