package hwacha
{
  import Chisel._
  import Node._
  import Interface._
  import hardfloat._

  /*
  class TopIo extends Bundle {
    val io = Bool(INPUT); // Here temporarily just so this will compile
  }

  class Top extends Component {
    override val io = new TopIo();
    val vuVMU_BHWDsel = new vuVMU_BHWD_sel();
    val vuVMU_Ctrl_ut_issue = new vuVMU_Ctrl_ut_issue();
  }
  */

  object top_main
  {
    def main(args: Array[String]) =
    {
      val boot_args = args ++ Array("--target-dir", "generated-src");
      //chiselMain(boot_args, () => new vuVMU());
      //chiselMain(boot_args, () => new vuVCU());
      //chiselMain(boot_args, () => new vuVXU());
      chiselMain(boot_args, () => new vu());
      //chiselMain(boot_args, () => new vu_dmem_arbiter());
      //chiselMain(boot_args, () => new vuVXU_Banked8_Expand);
      //chiselMain(boot_args, () => new vuVXU_Banked8_Fire);
      //chiselMain(boot_args, () => new vuVXU_Banked8_Seq);
      //chiselMain(boot_args, () => new vuVXU_Banked8_Hazard);
      //chiselMain(boot_args, () => new vuVMU());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_fma());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_imul());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_conv());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_alu());
      //chiselMain(boot_args, () => new vuVXU_Banked8_Bank());
      //chiselMain(boot_args, () => new vuVXU_Banked8_Lane());
      //chiselMain(boot_args, () => new vuVXU_Issue());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_vec_store());
      //chiselMain(boot_args, () => new vuVMU_BHWD_sel());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_vec_load_wb());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_vec());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_ut());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_ut_store());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_ut_wb());
      //chiselMain(boot_args, () => new vuVMU_ROQ_ut());
      //chiselMain(boot_args, () => new vuVMU_QueueCount());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_vec_top());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_ut_issue());
      //chiselMain(boot_args, () => new vuVMU_Ctrl_vec_load_issue());
      
    } 
  }
} 
