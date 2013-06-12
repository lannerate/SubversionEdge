/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.collabnet.svnedge.admin
import grails.test.GrailsUnitTestCase
import groovy.mock.interceptor.StubFor
import grails.plugin.executor.SessionBinderUtils
import org.hibernate.SessionFactory
import org.hibernate.Session

/**
 * Tests for the JobsInfoService
 */
class JobsInfoServiceTests extends GrailsUnitTestCase {

    def jobsInfoService
    def control

    public void setUp() {
        super.setUp()
        mockLogging(JobsInfoService)
        this.jobsInfoService = new JobsInfoService()
        def scheduler = new Expando()
        scheduler.getTriggersOfJob = {p1, p2 -> []}
        this.jobsInfoService.quartzScheduler = scheduler
        
        this.jobsInfoService.sessionFactory = new Expando() as SessionFactory
        control = new StubFor(grails.plugin.executor.SessionBinderUtils, true)
        control.demand.bindSession(10) { sf -> true }
        control.demand.unbindSession(10) { sf -> println "Unbound session"}
    }

    /**
     * validates that JobInfoService correctly registers running and finishing of jobs
     */
    public void testJobsRunningAndFinished() {
        control.use {

        def job = [dataMap: [id: "1"], run: {Thread.sleep(500)}]
        jobsInfoService.queueJob(job, new Date())
        job = [dataMap: [id: "2"], run: {Thread.sleep(1000)}]
        jobsInfoService.queueJob(job, new Date())
        Thread.sleep(250)
        assertEquals("there should be 2 running jobs according to the service", 2,
                jobsInfoService.runningJobs.size())

        Thread.sleep(500)
        assertEquals("there should be 1 running job according to the service", 1,
                jobsInfoService.runningJobs.size())
        assertEquals("there should be 1 finished job according to the service", 1,
                jobsInfoService.finishedJobs.size())

        Thread.sleep(1000)
        assertEquals("there should be 0 running job according to the service", 0,
                jobsInfoService.runningJobs.size())
        assertEquals("there should be 2 finished job according to the service", 2,
                jobsInfoService.finishedJobs.size())
        }
    }

    /**
     * validates that info of only 5 recent jobs are retained
     */
    public void testFinishedJobsPruning() {
        control.use {

        (1..8).each {
            def job = [dataMap: [id: "${it}"], run: {Thread.sleep(1000)}]
            jobsInfoService.queueJob(job, new Date())
        }
        Thread.sleep(400)
        assertEquals("there should be 3 running jobs according to the service", 3,
                jobsInfoService.runningJobs.size())
        assertEquals("there should be 5 scheduled jobs according to the service", 5,
            jobsInfoService.scheduledJobs.size())

        Thread.sleep(1000)
        assertEquals("there should be 3 running jobs according to the service", 3,
                jobsInfoService.runningJobs.size())
        assertEquals("there should be 3 finished jobs according to the service", 3,
            jobsInfoService.finishedJobs.size())
        assertEquals("there should be 2 scheduled jobs according to the service", 2,
            jobsInfoService.scheduledJobs.size())

        Thread.sleep(1000)
        assertEquals("there should be 2 running jobs according to the service", 2,
                jobsInfoService.runningJobs.size())
        assertEquals("there should be 5 finished jobs according to the service", 5,
            jobsInfoService.finishedJobs.size())
        assertEquals("there should be 0 scheduled jobs according to the service", 0,
            jobsInfoService.scheduledJobs.size())

        Thread.sleep(1000)
        assertEquals("there should be 0 running jobs according to the service", 0,
                jobsInfoService.runningJobs.size())
        assertEquals("there should be 5 finished jobs according to the service", 5,
            jobsInfoService.finishedJobs.size())
        assertEquals("there should be 0 scheduled jobs according to the service", 0,
            jobsInfoService.scheduledJobs.size())
        }
    }
    
    /**
    * validates that attempts to queue a job already in the queue will not
    * add a second copy.
    */
   public void testDuplicateJob() {
       control.use {

       Date now = new Date()
       (1..8).each {
           def job = [dataMap: [id: "${it}"], run: {Thread.sleep(500)}]
           jobsInfoService.queueJob(job, now)
       }
       Thread.sleep(100)
       assertEquals("there should be 3 running jobs according to the service", 3,
               jobsInfoService.runningJobs.size())
       assertEquals("there should be 5 scheduled jobs according to the service", 5,
               jobsInfoService.scheduledJobs.size())

       def job = [dataMap: [id: "8"], run: {Thread.sleep(500)}]
       jobsInfoService.queueJob(job, new Date())
       Thread.sleep(100)
       assertEquals("there should be 3 running jobs according to the service", 3,
               jobsInfoService.runningJobs.size())
       assertEquals("there should be 5 scheduled jobs according to the service", 5,
               jobsInfoService.scheduledJobs.size())
       assertEquals("later duplicate job should be skipped " + 
               jobsInfoService.scheduledJobs, now.time, 
               jobsInfoService.scheduledJobs["8"].scheduledFireTime.time)
       // Let the rest of the threads complete to avoid exceptions
       Thread.sleep(1500)
       }           
   }
}
