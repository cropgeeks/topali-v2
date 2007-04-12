<%= request.getQueryString() %>



<pre>

Current = 16

Release History:

[NF] = New Feature
[BF] = Bug Fix
[SC] = Source Code

  - [NF] The job status panel will now tell you what position in the queue (on
    the remote cluster) you job is currently in while waiting to run.
  - [NF] Improvements to job feedback (finer percentage levels) for LRT and DSS
    analysis jobs.
  - [NF] The old "Partitions" dialog has been split into a generic annotations
    viewer, with separate sections for Paritions and Coding Regions (so far).
  - [NF] Alignment selections can now be carried out using the main alignment
    view window.
  - [NF] Cleaned up the job progress screen, allowing for a double-click to
    cancel a job.
  - [BF] TOPALi is now compatible with Java 1.6 after fixing multidimensional
    array issues with the Castor XML package. This has changed the XML project
    format again though, so projects created in versions earlier than 2.16 will
    no longer load. Project files have also been reduced in size.
  - [BF] Updated the timer threads for the job progress screen so that jobs with
    indeterminate progress bars are properly displayed.
  - [NF] Updated the export dialog so alignments can now be created/exported by
    concatenating together one or more of the currently selected partitions.
  - [NF] Updated help for several topics.
  - [NF] Improved the rendering speed of the charts while highlighting regions.
  - [NF] TOPALi can now use ReadSeq (remotely) for additional sequence-format
    compatibility.

18-09-2006 - Release 2.15
  - [SC] VAMSAS integration code enabled again.
  - [SC] Changes and updates to the library JAR files.
  - [NF] TOPALi can now use a remote resource broker to determine which HPC
    cluster it should submit job requests to.
  - [BF] Clicking on the Results folder in the treeview will now give a proper
    indication of result status (how many, and how many completed/in progress).

18-08-2006 - Release 2.14
  - [BF] Clustering sequences into a group (using trees) should now select the
    correct set of sequences back in the main panel.
  - [NF] Added help documentation for the Settings dialog.

04-08-2006 - Release 2.13
  - [NF] Locally run jobs are now queued so that they do not all request CPU
    resource at the same time.
  - [NF] Added a new tab to the Settings dialog that allows you to clear the
    disk cache (left by local jobs) and to set the maximum number of CPUs or
    cores you want TOPALi to use for local jobs.

26-07-2006 - Release 2.12
  - [BF] Fixed a bug with HMM topology defining sites creating invalid input to
    to Barce.

13-07-2006 - Release 2.11
  - [BF] Reinstated the burn-time control for PDM analysis jobs.

10-07-2006 - Release 2.10
  - [NF] Added support for loading alignments in Muscle format.
  - [NF] PDM now calculates its significance threshold via bootstrapping (in
    parallel over the cluster).

10-05-2006 - Release 2.09
  - [BF] Modified the way cluster job progress is tracked as a workaround for a
    segmentation fault problem with qstat at Dundee Uni.
  - [NF] Alignments can now be imported by creating a new alignment from an
    unaligned cDNA file and its guide protein file.
  - [NF] Support for sequence clustering on trees has been added.
  - [NF] Added support for authenticating proxy servers.
  - [SC] Modified doe.jar so that Preference saving handles empty strings.
  - [BF] Alignments sent to Barce are now checked for compatibility.
  - [BF] Default setting of 0 for web proxy port was too low for the spinner
    control.
  - [SC] Removed support for compression as it wasn't working in all cases.

07-04-2006 - Release 2.08
  - [NF] Matched TOPALi's gradient panels for the Office Look and Feel to those
    of Windows for Classic, Blue, Silver, and Olive colour schemes.
  - [NF] Full support for local job submission is now included.
  
06-04-2006 - Release 2.07
  - [NF] Added client-side support for server-side gzip compression, reducing
    the amount of data sent between client and server.

05-04-2006 - Release 2.06
  - [BF] The web proxy port option in the settings dialog was not being
    maintained between sessions.
  - [BF] LRT analyses were using all sequences rather than the selected set.
  - [NF] Warning about closing a project while analysis jobs are running.
  - [BF] The status bar now correctly shows job information when changing
    project (and jobs are still running).
  - [SC] DRMAA now used for cluster control (which should also reduce the number
    of "unknown job status" errors returned by the job monitoring code.
  - [BF] Fixed a bug where the output from a qsub job submission was read before
    the buffer was complete, which resulted in a failed submission.
  - [BF] Fixed a bug where PDM and HMM jobs could cause an XML premature end of
  - file error due to the webservice trying to read the result object as it was
    still being written.
  - [SC] Separate configuration files for multiple cluster deployment.
  - [SC] Additional logging of web services/remote jobs using Java Logging API,
    including logging of access requests and job types.

09-03-2006 - Release 2.05
  - [NF] All alignment graph result types can be auto-partitioned.
  - [NF] HMM partitioning ignores indeterminate regions.
  - [SC] Refactoring of the internal data structures for XML (required for a lot
    of bug fixes but unfortunately not backwards compatible).
  - [BF] The Partitions Dialog and the Overview Dialog both now correctly update
    their state when datasets are added or removed.
  - [NF] The status of each alignment within a project is shown as the project
    loads.

08-02-2006 - Release 2.04
  - [BF] Saved projects containing trees will now reload OK.
  - [BF] Trees were not being created as mid-point rooted.

03-02-2006 - Release 2.03
  - [BF] Fix for non-English users (resource bundles not loading).

31-01-2006 - Release 2.02
  - [BF] HMM graphs now have their topologies labelled properly.
  - [NF] Added the ability to view "live" phylogenetic tree previews as the
    mouse cursor is moved over a result graph. Enable the setting from the
    toolbar on the right.
  - [SC] Graph-related result objects now extend from AlignmentResult rather
    than AnalysisResult.

16-01-2006 - Release 2.01
  - Initial public release.
  
</pre>