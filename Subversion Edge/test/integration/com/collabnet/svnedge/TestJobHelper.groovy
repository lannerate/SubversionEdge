package com.collabnet.svnedge

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

import org.quartz.JobListener
import org.quartz.JobExecutionContext

class TestJobHelper implements JobListener {
    def log
    def executorService
    def quartzScheduler
    String listenerName
    def job
    boolean jobIsFinished = false
    
    void executeJob(params) {
        def context = [getMergedJobDataMap: { params }]
        job.execute(context)
        /*        
        executorService.execute({
            quartzScheduler.start()
            quartzScheduler.addGlobalJobListener(this)
            // make sure our job is unpaused
            quartzScheduler.resumeJobGroup(job.getGroup())
            job.triggerNow(params)
        } as Runnable)
        this.waitForJob()
        quartzScheduler.standby()
        */
    }
    
    synchronized void waitForJob() {
        if (jobIsFinished) {
            log.info("Job is finished, no need to wait.")
        } else {
            log.info("Job triggered; waiting to finish...")
            log.info("Before wait")
            threadDump()
            this.wait(60000)
            log.info("After wait")
            threadDump()
            log.info("Wait is over! Continuing test")
        }
    }
    
    private synchronized void notifyOnFinishedJob() {
        jobIsFinished = true
        log.info("Job is done!")
        this.notify()
    } 
    
    private void threadDump() {
        def title = "Thread dump:\n"
        boolean lockedMonitors = true
        boolean lockedOwnableSynchronizers = true
        ThreadInfo[] tinfos = ManagementFactory.getThreadMXBean()
                .dumpAllThreads(lockedMonitors, lockedOwnableSynchronizers)
        StringBuilder sb = new StringBuilder()
        for (ThreadInfo tinfo : tinfos ) {
            sb.append(tinfo)
            MonitorInfo[] minfos = tinfo.getLockedMonitors()
            if (minfos) {
                sb.append("    locked monitors:\n")
                for (MonitorInfo minfo : minfos) {
                    sb.append("      ").append(minfo)
                }
                sb.append("\n")
            }
            LockInfo[] sinfos = tinfo.getLockedSynchronizers()
            if (sinfos) {
                sb.append("    locked synchronizers:\n")
                for(LockInfo sinfo : sinfos) {
                    sb.append("      ").append(sinfo)
                }
                sb.append("\n")
            }
            sb.append("\n")
        }
        log.info(title + sb.toString())
    }

    /** Listener methods **/
    public String getName() {
        return listenerName
    }
    
    void jobToBeExecuted(JobExecutionContext context) {}
    
    void jobExecutionVetoed(JobExecutionContext context) {
        notifyOnFinishedJob()
        throw new RuntimeException("Did not expect job to be vetoed.")
    }

    void jobWasExecuted(JobExecutionContext context,
                        org.quartz.JobExecutionException jobException) {
        if (context.getJobDetail().getName().equals(job.name)) {
            notifyOnFinishedJob()
        }
    }
}
