package hwacha

import Chisel._
import Node._
import Constants._

class io_vxu_issue_fire extends Bundle
{
  val viu = Bool()
  val vau0 = Bool()
  val vau1 = Bool()
  val vau2 = Bool()
  val amo = Bool()
  val utld = Bool()
  val utst = Bool()
  val vld = Bool()
  val vst = Bool()
}

class io_vxu_issue_fu extends Bundle
{
  val viu = Bool()
  val vau0 = Bool()
  val vau1 = Bool()
  val vau2 = Bool()
  val vgu = Bool()
  val vlu = Bool()
  val vsu = Bool()
}

class io_vxu_issue_fn extends Bundle
{
  val viu = new VIUFn
  val vau0 = new VAU0Fn
  val vau1 = new VAU1Fn
  val vau2 = new VAU2Fn
  val vmu = new VMUFn
}

class io_vxu_issue_reg extends Bundle
{
  val vs = Bool()
  val vt = Bool()
  val vr = Bool()
  val vd = Bool()
}

class io_vxu_cnt_valid extends ValidIO(Bits(width = SZ_VLEN) )

class io_vxu_issue_regid_imm extends Bundle
{
  val vlen = UInt(width = SZ_VLEN)
  val utidx = UInt(width = SZ_VLEN)
  val vs_zero = Bool()
  val vt_zero = Bool()
  val vr_zero = Bool()
  val vd_zero = Bool()
  val vs = Bits(width = SZ_BREGLEN)
  val vt = Bits(width = SZ_BREGLEN)
  val vr = Bits(width = SZ_BREGLEN)
  val vd = Bits(width = SZ_BREGLEN)
  val rtype = Bits(width = 4)
  val vs_active = Bool()
  val vt_active = Bool()
  val vr_active = Bool()
  val vd_active = Bool()
  val imm = Bits(width = SZ_DATA)
  val imm2 = Bits(width = SZ_DATA)
  val cnt_valid = Bool()
  val cnt = Bits(width = SZ_VLEN)
  val aiw = new io_vxu_aiw_bundle()
}

class io_vxu_issue_op extends Bundle
{
  val r1w1 = Bool()
  val r2w1 = Bool()
  val r3w1 = Bool()
  val amo = Bool()
  val utld = Bool()
  val utst = Bool()
  val vld = Bool()
  val vst = Bool()
}

class io_vxu_issue_to_hazard extends Bundle
{
  val bcnt = Bits(width = SZ_BCNT)
  val stride = Bits(width = SZ_REGLEN) // just for vd, stride known whether X or F
}

class io_vxu_issue_to_seq extends Bundle
{
  val vlen = Bits(width = SZ_VLEN)
  val xf_split = Bits(width = SZ_BANK)
  val xstride = Bits(width = SZ_REGLEN)
  val fstride = Bits(width = SZ_REGLEN)
  val bcnt = Bits(width = SZ_BCNT)
}

class io_vxu_issue_to_lane extends Bundle
{
  val bactive = Bits(width = SZ_BANK)
}

class io_issue_to_aiw extends Bundle
{
  val markLast = Bool(OUTPUT)
  val update_numCnt = new io_update_num_cnt()
}

class io_issue_to_irq_handler extends Bundle
{
  val tvec = new io_issue_tvec_to_irq_handler()
  val vt = new io_issue_vt_to_irq_handler()
}

class IssueOpIO extends ValidIO(new IssueOp)

class Issue(resetSignal: Bool = null)(implicit conf: HwachaConfiguration) extends Module(_reset = resetSignal)
{
  val io = new Bundle {
    val cfg = new HwachaConfigIO

    val irq = new io_issue_to_irq_handler()
    val imem = new rocket.CPUFrontendIO()(conf.vicache)
    val vcmdq = new VCMDQIO().flip

    val issue_to_hazard = new io_vxu_issue_to_hazard().asOutput
    val issue_to_lane = new io_vxu_issue_to_lane().asOutput

    val tvec_valid = new io_vxu_issue_fire().asOutput
    val tvec_ready = Bool(INPUT)
    val tvec_dhazard = new io_vxu_issue_reg().asOutput
    val tvec_shazard = new io_vxu_issue_fu().asOutput
    val tvec_bhazard = new io_vxu_issue_op().asOutput
    val tvec_fn = new io_vxu_issue_fn().asOutput
    val tvec_regid_imm = new io_vxu_issue_regid_imm().asOutput

    val vt_valid = new io_vxu_issue_fire().asOutput
    val vt_ready = Bool(INPUT)
    val vt_dhazard = new io_vxu_issue_reg().asOutput
    val vt_shazard = new io_vxu_issue_fu().asOutput
    val vt_bhazard = new io_vxu_issue_op().asOutput
    val vt_fn = new io_vxu_issue_fn().asOutput
    val vt_regid_imm = new io_vxu_issue_regid_imm().asOutput
    
    val pending_vf = Bool(OUTPUT)

    val aiw_cmdb = new io_vxu_cmdq()
    val aiw_imm1b = new io_vxu_immq()
    val aiw_imm2b = new io_vxu_imm2q()
    val aiw_cntb = new io_vxu_cntq()
    val aiw_numCntB = new io_vxu_numcntq()

    val issue_to_aiw = new io_issue_to_aiw()
    val aiw_to_issue = new io_aiw_to_issue().flip

    val xcpt_to_issue = new io_xcpt_handler_to_issue().flip()
  }

  val tvec = Module(new IssueTVEC)
  val vt = Module(new IssueVT)

  io.cfg <> tvec.io.cfg
  vt.io.cfg <> tvec.io.cfg
  vt.io.vf <> tvec.io.vf
  io.pending_vf := tvec.io.vf.active

  io.irq.tvec := tvec.io.irq
  io.irq.vt := vt.io.irq

  vt.io.imem <> io.imem

  io.vcmdq.cnt.ready := tvec.io.vcmdq.cnt.ready || vt.io.vcmdq.cnt.ready

  // tvec
  tvec.io.vcmdq.cmd <> io.vcmdq.cmd
  tvec.io.vcmdq.imm1 <> io.vcmdq.imm1
  tvec.io.vcmdq.imm2 <> io.vcmdq.imm2
  tvec.io.vcmdq.cnt.valid := io.vcmdq.cnt.valid
  tvec.io.vcmdq.cnt.bits := io.vcmdq.cnt.bits

  tvec.io.issue_to_hazard <> io.issue_to_hazard
  tvec.io.issue_to_lane <> io.issue_to_lane

  tvec.io.valid <> io.tvec_valid
  tvec.io.ready <> io.tvec_ready
  tvec.io.dhazard <> io.tvec_dhazard
  tvec.io.shazard <> io.tvec_shazard
  tvec.io.bhazard <> io.tvec_bhazard
  tvec.io.fn <> io.tvec_fn
  tvec.io.decoded <> io.tvec_regid_imm

  tvec.io.aiw_cmdb <> io.aiw_cmdb
  tvec.io.aiw_imm1b <> io.aiw_imm1b
  tvec.io.aiw_imm2b <> io.aiw_imm2b
  tvec.io.aiw_cntb.ready := io.aiw_cntb.ready
  tvec.io.aiw_numCntB <> io.aiw_numCntB
  tvec.io.aiw_to_issue <> io.aiw_to_issue

  tvec.io.xcpt_to_issue <> io.xcpt_to_issue

  // vt
  vt.io.valid <> io.vt_valid
  vt.io.ready <> io.vt_ready
  vt.io.dhazard <> io.vt_dhazard
  vt.io.shazard <> io.vt_shazard
  vt.io.bhazard <> io.vt_bhazard
  vt.io.fn <> io.vt_fn
  vt.io.decoded <> io.vt_regid_imm

  vt.io.vcmdq.cnt.valid := io.vcmdq.cnt.valid
  vt.io.vcmdq.cnt.bits := io.vcmdq.cnt.bits

  vt.io.aiw_cntb.ready := io.aiw_cntb.ready
  vt.io.aiw_to_issue <> io.aiw_to_issue
  vt.io.issue_to_aiw.update_numCnt <> io.issue_to_aiw.update_numCnt

  io.issue_to_aiw.markLast := 
    Mux(tvec.io.active, tvec.io.issue_to_aiw.markLast, vt.io.issue_to_aiw.markLast)

  vt.io.xcpt_to_issue <> io.xcpt_to_issue

  io.aiw_cntb.valid := Mux(tvec.io.active, tvec.io.aiw_cntb.valid, vt.io.aiw_cntb.valid)
  io.aiw_cntb.bits := Mux(tvec.io.active, tvec.io.aiw_cntb.bits, vt.io.aiw_cntb.bits)
}
