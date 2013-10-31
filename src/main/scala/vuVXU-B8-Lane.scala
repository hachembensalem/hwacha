package hwacha

import Chisel._
import Node._
import Constants._
import scala.collection.mutable.ArrayBuffer

class CPIO extends Bundle
{
  val imul_val = Bool(INPUT)
  val imul_rdy = Bool(OUTPUT)
  val imul_fn  = Bits(INPUT, SZ_VAU0_FN)
  val imul_in0 = Bits(INPUT, SZ_XLEN)
  val imul_in1 = Bits(INPUT, SZ_XLEN)
  val imul_out = Bits(OUTPUT, SZ_XLEN)
}

class ExpanderIO extends Bundle
{
  val bank = new BankToBankIO().flip
  val lfu = new ExpanderToLFUIO()
}

class VMUIO extends Bundle 
{
  val vaq_val   = Bool(OUTPUT)
  val vaq_check = new io_vxu_mem_check().asOutput
  val vaq_mem = new io_vxu_mem_cmd().asOutput
  val vaq_imm = Bits(OUTPUT, SZ_DATA)
  val vaq_utmemop = Bool(OUTPUT)
  val vaq_rf = Bits(OUTPUT, SZ_DATA)

  val vldq_rdy  = Bool(OUTPUT)
  val vldq_bits = Bits(INPUT, SZ_DATA)
  val vsdq_val  = Bool(OUTPUT)
  val vsdq_mem = new io_vxu_mem_cmd().asOutput
  val vsdq_bits = Bits(OUTPUT, SZ_DATA)
}

class io_lane_to_hazard extends Bundle
{
  val rlast = Bool()
  val wlast = Bool()
  val wlast_mask = Bool()
  val pvfb_tag = Bits(width=SZ_PVFB_TAG)
}

class ioLaneToPVFB extends Bundle
{
  val mask = Valid(Bits(width=WIDTH_PVFB) ) 
}

class ioLaneToIssue extends Bundle
{
  val mask = Valid(Bits(width=WIDTH_PVFB * NUM_PVFB) )
  val pvfb_tag = Bits(OUTPUT, SZ_PVFB_TAG)
}

class io_vxu_lane extends Bundle 
{
  val cp = new CPIO()
  val cp_dfma = new io_cp_dfma()
  val cp_sfma = new io_cp_sfma()

  val issue_to_lane = new io_vxu_issue_to_lane().asInput
  val expand_read = new io_vxu_expand_read().asInput
  val expand_write = new io_vxu_expand_write().asInput
  val expand_fu_fn = new io_vxu_expand_fu_fn().asInput
  val expand_lfu_fn = new io_vxu_expand_lfu_fn().asInput
  val lane_to_hazard = new io_lane_to_hazard().asOutput
  val laneToIssue = new ioLaneToIssue()
  val vmu = new VMUIO()
}

class vuVXU_Banked8_Lane extends Module
{
  val io = new io_vxu_lane()

  val conn = new ArrayBuffer[BankToBankIO]
  var first = true

  val rblen = new ArrayBuffer[UInt]
  val rdata = new ArrayBuffer[UInt]
  val ropl0 = new ArrayBuffer[UInt]
  val ropl1 = new ArrayBuffer[UInt]

  val masks = new ArrayBuffer[Bits]

  //forward declaring imul, fma, and conv units
  val imul = Module(new vuVXU_Banked8_FU_imul)
  val fma  = Module(new vuVXU_Banked8_FU_fma)
  val conv = Module(new vuVXU_Banked8_FU_conv)

  for (i <- 0 until SZ_BANK) 
  {
    val bank = Module(new vuVXU_Banked8_Bank)
    bank.io.active := io.issue_to_lane.bactive(i)
    
    if (first)
    { 
      bank.io.in <> io.expand_read 
      bank.io.in <> io.expand_write
      bank.io.in <> io.expand_fu_fn
      first = false 
    } 
    else 
    {
      bank.io.in <> conn.last
    }
    
    conn  += bank.io.out
    rblen += bank.io.rw.rblen
    rdata += bank.io.rw.rdata
    ropl0 += bank.io.rw.ropl0
    ropl1 += bank.io.rw.ropl1
    masks += bank.io.branch_resolution_mask

    bank.io.rw.wbl0 := imul.io.out
    bank.io.rw.wbl1 := fma.io.out
    bank.io.rw.wbl2 := conv.io.out
    bank.io.rw.wbl3 := io.vmu.vldq_bits

  }

  def calcMask(n: Int): Bits = {
    val strip = Cat(masks.map(x => x(n)).reverse) 
    if(n == 0)
      strip
    else
      Cat(strip, calcMask(n-1))
  }

  val mask = calcMask(WIDTH_BMASK-1)

  io.laneToIssue.mask.bits := mask
  io.laneToIssue.mask.valid := conn.last.wlast_mask
  io.laneToIssue.pvfb_tag := conn.last.pvfb_tag

  io.lane_to_hazard.rlast := conn.last.rlast
  io.lane_to_hazard.wlast := conn.last.wlast
  io.lane_to_hazard.wlast_mask := conn.last.wlast_mask
  io.lane_to_hazard.pvfb_tag := conn.last.pvfb_tag

  val xbar = Module(new vuVXU_Banked8_Lane_Xbar)
  xbar.io.rblen <> rblen
  xbar.io.rdata <> rdata
  xbar.io.ropl0 <> ropl0
  xbar.io.ropl1 <> ropl1
  val rbl = xbar.io.rbl

  val lfu = Module(new vuVXU_Banked8_Lane_LFU)

  lfu.io.expand_rcnt := io.expand_read.rcnt.toUInt
  lfu.io.expand_wcnt := io.expand_write.wcnt.toUInt
  lfu.io.expand <> io.expand_lfu_fn

  val vau0_val  = lfu.io.vau0_val
  val vau0_fn   = lfu.io.vau0_fn
  val vau1_val  = lfu.io.vau1_val
  val vau1_fn   = lfu.io.vau1_fn
  val vau2_val  = lfu.io.vau2_val
  val vau2_fn   = lfu.io.vau2_fn

  io.vmu.vaq_val   := lfu.io.vaq_val
  io.vmu.vaq_check <> lfu.io.vaq_check
  io.vmu.vaq_mem <> lfu.io.vaq_mem
  io.vmu.vaq_imm := lfu.io.vaq_imm
  io.vmu.vaq_utmemop := lfu.io.vaq_utmemop

  io.vmu.vldq_rdy  := lfu.io.vldq_rdy
  io.vmu.vsdq_val  := lfu.io.vsdq_val
  io.vmu.vsdq_mem <> lfu.io.vsdq_mem

  val imul_fn  = Mux(vau0_val, vau0_fn, io.cp.imul_fn)
  val imul_in0 = Mux(vau0_val, rbl(0), Cat(Bits(0,1), io.cp.imul_in0))
  val imul_in1 = Mux(vau0_val, rbl(1), Cat(Bits(0,1), io.cp.imul_in1))

  io.cp.imul_rdy := ~vau0_val
  io.cp.imul_out := imul.io.out(SZ_XLEN-1,0)

  //integer multiply
  imul.io.valid := vau0_val | io.cp.imul_val
  imul.io.fn  := imul_fn
  imul.io.in0 := imul_in0
  imul.io.in1 := imul_in1

  //fma
  fma.io.valid := vau1_val
  fma.io.fn    := vau1_fn
  fma.io.in0   := rbl(2)
  fma.io.in1   := rbl(3)
  fma.io.in2   := rbl(4)

  io.cp_dfma <> fma.io.cp_dfma
  io.cp_sfma <> fma.io.cp_sfma

  //conv
  conv.io.valid := vau2_val
  conv.io.fn := vau2_fn
  conv.io.in := rbl(5)

  io.vmu.vaq_rf := rbl(6)(SZ_ADDR-1,0)
  io.vmu.vsdq_bits := rbl(7)
}