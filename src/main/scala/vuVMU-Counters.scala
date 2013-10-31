package hwacha

import Chisel._
import Node._
import Constants._

class io_vmu_counters extends Bundle
{
  val vvaq_inc = Bool(INPUT)
  val vvaq_dec = Bool(INPUT)
  val vpaq_inc = Bool(INPUT)
  val vpaq_dec = Bool(INPUT)
  val vsdq_inc = Bool(INPUT)
  val vsdq_dec = Bool(INPUT)
  val vpasdq_inc = Bool(INPUT)
  val vpasdq_dec = Bool(INPUT)
  val vlreq_inc = Bool(INPUT)
  val vlreq_dec = Bool(INPUT)
  val vsreq_inc = Bool(INPUT)
  val vsreq_dec = Bool(INPUT)

  val qcnt = UInt(INPUT, SZ_QCNT)
  val vvaq_watermark = Bool(OUTPUT)
  val vsdq_watermark = Bool(OUTPUT)
  val vpasdq_watermark = Bool(OUTPUT)
  val vlreq_watermark = Bool(OUTPUT)
  val vsreq_watermark = Bool(OUTPUT)

  val vpaq_qcnt = UInt(INPUT, SZ_QCNT)
  val vpaq_watermark = Bool(OUTPUT)

  val pending_load = Bool(OUTPUT)
  val pending_store = Bool(OUTPUT)
}

class vuVMU_Counters extends Module
{
  val io = new io_vmu_counters()

  val vvaq_count = Module(new qcnt(ENTRIES_VVAQ, ENTRIES_VVAQ))
  val vpaq_count = Module(new qcnt(0, ENTRIES_VPAQ))
  val vsdq_count = Module(new qcnt(ENTRIES_VSDQ,ENTRIES_VSDQ))
  val vpasdq_count = Module(new qcnt(0, ENTRIES_VPASDQ))
  val vsreq_count = Module(new qcnt(ENTRIES_VSREQ, ENTRIES_VSREQ)) // vector stores in flight
  val vlreq_count = Module(new qcnt(ENTRIES_VLREQ, ENTRIES_VLREQ)) // vector loads in flight

  // vvaq counts available space
  vvaq_count.io.inc := io.vvaq_inc
  vvaq_count.io.dec := io.vvaq_dec
  vvaq_count.io.qcnt := io.qcnt
  io.vvaq_watermark := vvaq_count.io.watermark

  // vpaq counts occupied space
  vpaq_count.io.inc := io.vpaq_inc
  vpaq_count.io.dec := io.vpaq_dec
  vpaq_count.io.qcnt := io.vpaq_qcnt
  io.vpaq_watermark := vpaq_count.io.watermark

  // vsdq counts available space
  vsdq_count.io.inc := io.vsdq_inc
  vsdq_count.io.dec := io.vsdq_dec
  vsdq_count.io.qcnt := io.qcnt
  io.vsdq_watermark := vsdq_count.io.watermark

  // vpasdq counts occupied space
  vpasdq_count.io.inc := io.vpasdq_inc
  vpasdq_count.io.dec := io.vpasdq_dec
  vpasdq_count.io.qcnt := io.qcnt
  io.vpasdq_watermark := vpasdq_count.io.watermark

  // vlreq counts available space
  vlreq_count.io.inc := io.vlreq_inc
  vlreq_count.io.dec := io.vlreq_dec
  vlreq_count.io.qcnt := io.qcnt
  io.vlreq_watermark := vlreq_count.io.watermark

  // vsreq counts available space
  vsreq_count.io.inc := io.vsreq_inc
  vsreq_count.io.dec := io.vsreq_dec
  vsreq_count.io.qcnt := io.qcnt
  io.vsreq_watermark := vsreq_count.io.watermark

  // there is no loads in flight, when the counter is full
  io.pending_load := !vlreq_count.io.full
  // there is no stores in flight, when the counter is full
  io.pending_store := !vsreq_count.io.full
}