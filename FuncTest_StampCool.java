final class FuncTest_StampCool extends TestItem {

  FuncTest_StampCool()
	throws NoSuchConditionException {

		StampParse.showLog = false;
		stmp = new StampParse[KTestSystem.getDefinedDutCount()];
		for(int dut : KTestSystem.getDut(KDutGroupType.CDUT)) stmp[dut-1] = new StampParse();

		this.pSize = StampParse.is16KMode() ? DeviceInfo.NANDCOLS_16K      : DeviceInfo.NANDCOLS_8K;
		this.pds   = StampParse.is16KMode() ? new PDSGroup("Standard_16k") : new PDSGroup("Standard_8k"); this.pds.set();

	}

	@Override
	public void body() throws NoMoreTargetDutException {


		System.out.println("COL MODE is set to "+(StampParse.is16KMode()?"16K":"8K"));
		System.out.println("DEFAULT BIN is "+StampParse.defaultBin());


		CommonRoutines.columnMode = StampParse.is16KMode() ? CommonRoutines.Mode16k : CommonRoutines.Mode8k;
		try{
			this.pat_1byte = new Pattern(patName, 0x0008, CommonRoutines.columnMode);
			this.pat_Nbyte = new Pattern(patName, 0x0010, CommonRoutines.columnMode);
		}catch(Exception e){ throw new RuntimeException(e); }

		Pattern pat = null;

		seq.on();
		for(StampInfo si : StampParse.stamps()){

			String stamp_name   = si.name;
			String group        = si.group;

			int start_col	= si.startCol;
			int col_cnt	= si.colCnt;
			int page	= si.page;
			int exp		= si.exp;

			pat = col_cnt > 1 ? pat_Nbyte : pat_1byte;
			pat.registerList.clear();
			pat.registerList.add(new KRegister(KRegisterType.D2B, page));
			pat.registerList.add(new KRegister(KRegisterType.D2C, start_col));
			pat.registerList.add(new KRegister(KRegisterType.TPH, exp)); //stamp byte value
			if( col_cnt > 1) pat.registerList.add(new KRegister(KRegisterType.IDX5, col_cnt - 2)); //# of stamp bytes
			pat.registerList.add(new KRegister(KRegisterType.IDX1, start_col - 2));
			pat.registerList.add(new KRegister(KRegisterType.IDX6, pSize - start_col - 2 - col_cnt));
			
			pat.apply();
			pat.execute();

			for(int dut : KTestSystem.getDut(KDutGroupType.CDUT)){
				if( (0xfff00000 & Utility.readPinFail(dut, (dut%2==1) ? 0x1 : 0x2))==0 ){ //if found stamp
					stmp[dut-1].inc( si.group);
				}
			}
		}
		seq.off();
	
		// Bin out
		for(int dut : KTestSystem.getDut(KDutGroupType.CDUT)){
			System.out.printf("\nDUT%02d -->\n", dut);
			int hardbin = stmp[dut-1].findBin();
			KSort.write(dut, hardbin);
			SS.dutInfo.DUTList[dut - 1].currentTestPF = DUTInfo.PF.FAIL;
			SS.dutInfo.addFailedTest(dut, number);
			SS.dutInfo.setSortNumber(dut, hardbin);
			DutExclusion.setPermanent( dut);
		}

	}

}

