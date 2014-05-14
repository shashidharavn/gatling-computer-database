package models

import java.util.Date

import play.api.db._
import play.api.Play.current

import anorm._

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListSet}
import java.util.concurrent.atomic.AtomicLong
import scala.collection.JavaConversions._

case class Company(id: Long, name: String) 
case class Computer(id: Pk[Long] = NotAssigned, name: String, introduced: Option[Date], discontinued: Option[Date], companyId: Option[Long]) 

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Computer {

  val nextId = new AtomicLong()
  val allComputers = new ConcurrentHashMap[Long, Computer]
  val sortedComputers = new ConcurrentSkipListSet[Computer](new java.util.Comparator[Computer]{
      override def compare(o1: Computer,  o2: Computer) = {
        val nameCmp = o1.name.compareTo(o2.name)
        if(nameCmp == 0) {
          val id1: Long = o1.id.getOrElse(-1l)
          val id2: Long = o2.id.getOrElse(-1l)
          id1.compareTo(id2)
        }
        else 
          nameCmp
      }
    })

  /**
   * Retrieve a computer from the id.
   */
  def findById(id: Long): Option[Computer] = Option(allComputers.get(id))
  
  /**
   * Return a page of (Computer,Company).
   *
   * @param page Page to display
   * @param pageSize Number of computers per page
   * @param orderBy Computer property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[(Computer, Option[Company])] = {
    val offset = pageSize * page

    val possibleComputers = sortedComputers.iterator

    val applyFilter = filter != "%" && filter != "%%"
    val containsFilter = filter.replace("%", "")
    val filteredComputers: Iterator[Computer] =  
      if(applyFilter) possibleComputers.filter( (c) => c.name.toLowerCase.contains(containsFilter.toLowerCase) )
      else possibleComputers
    val computers = filteredComputers.drop(offset).take(pageSize).toVector

    Page(computers.map(c => (c, c.companyId.flatMap(Company.allCompanies.get(_)))), page, offset, if(applyFilter) computers.size + filteredComputers.size else sortedComputers.size)
  }
  
  /**
   * Update a computer.
   *
   * @param id The computer id
   * @param computer The computer values.
   */
  def update(id: Long, computer: Computer) {
    val oldComputer = findById(id)
    sortedComputers.remove(oldComputer.get)

    val computerWithId = computer.copy(id=Id(id))   
    sortedComputers.add(computerWithId)
    allComputers.replace(id, computerWithId)
  }
  
  /**
   * Insert a new computer.
   *
   * @param computer The computer values.
   */
  def insert(computer: Computer) {
    val id = nextId.incrementAndGet

    val computerWithId = computer.copy(id=Id(id))
    sortedComputers.add(computerWithId)
    allComputers.put(id, computerWithId)
  }
  
  /**
   * Delete a computer.
   *
   * @param id Id of the computer to delete.
   */
  def delete(id: Long) {
    findById(id).map(sortedComputers.remove(_))
    allComputers.remove(id)
  }

  def deleteAll() {
    allComputers.clear()
    sortedComputers.clear()
    nextId.set(0l)
  }
}

object Company {
  
  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  val options: Seq[(String,String)] = Seq( ("1","Apple Inc."), ("2","Thinking Machines"), ("3","RCA"), ("4","Netronics"), ("5","Tandy Corporation"), ("6","Commodore International"), ("7","MOS Technology"), ("8","Micro Instrumentation and Telemetry Systems"), ("9","IMS Associates, Inc."),( "10","Digital Equipment Corporation"),( "11","Lincoln Laboratory"),( "12","Moore School of Electrical Engineering"),( "13","IBM"),( "14","Amiga Corporation"),( "15","Canon"),( "16","Nokia"),( "17","Sony"),( "18","OQO"),( "19","NeXT")    ,( "20","Atari"),( "22","Acorn computer"),( "23","Timex Sinclair"),( "24","Nintendo"),( "25","Sinclair Research Ltd"),( "26","Xerox"),( "27","Hewlett-Packard"),( "28","Zemmix"),( "29","ACVS"),( "30","Sanyo"),( "31","Cray"),( "32","Evans & Sutherland")    ,( "33","E.S.R. Inc."),( "34","OMRON"),( "35","BBN Technologies"),( "36","Lenovo Group"),( "37","ASUS"),( "38","Amstrad"),( "39","Sun Microsystems"),( "40","Texas Instruments"),( "41","HTC Corporation"),( "42","Research In Motion"),( "43","Samsung Electronics"))

  val allCompanies: Map[Long, Company] = options.map {case (id, name) => (id.toLong, Company(id.toLong, name))} .toMap
}











