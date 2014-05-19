/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package datafu.test.pig.hash;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.pig.pigunit.PigTest;
import org.testng.annotations.Test;

import datafu.test.pig.PigTests;

public class HashTests extends PigTests
{
  /**
  

  define MD5 datafu.pig.hash.MD5();
  
  data_in = LOAD 'input' as (val:chararray);
  
  data_out = FOREACH data_in GENERATE MD5(val) as val;
  
  STORE data_out INTO 'output';
   */
  @Multiline private String md5Test;
  
  @Test
  public void md5Test() throws Exception
  {
    PigTest test = createPigTestFromString(md5Test);
    
    writeLinesToFile("input", 
                     "ladsljkasdglk",
                     "lkadsljasgjskdjks",
                     "aladlasdgjks",
                     "has_lo_md5_1065433"
                     );
            
    test.runScript();
        
    assertOutput(test, "data_out",
                 "(d9a82575758bb4978949dc0659205cc6)",
                 "(9ec37f02fae0d8d6a7f4453a62272f1f)",
                 "(cb94139a8b9f3243e68a898ec6bd9b3d)",
                 "(000008e5487b3abae7be88a1d4bad573)");
  }
  
  /**
  
  
  define SHA256 datafu.pig.hash.SHA('256');
  
  data_in = LOAD 'input' as (val:chararray);
  
  data_out = FOREACH data_in GENERATE SHA256(val) as val;
  
  STORE data_out INTO 'output';
   */
  @Multiline private String sha256Test;
  
  @Test
  public void sha256Test() throws Exception
  {
	  PigTest test = createPigTestFromString(sha256Test);
	  
	  writeLinesToFile("input", 
			  		   "ladsljkasdglk",
                       "lkadsljasgjskdjks",
                       "has_lo_md5_1065433",
                       "has_lo_sha256_11542105");
	  
	  test.runScript();
	  
	  assertOutput(test, "data_out", 
			  "(70ebaf99c4d8ff8860869e50be2d46afbf150b883f66b50a76ee81cdc802242b)",
			  "(f22e3c744a9ade0fa591d28c55392035248b391c9ee4c77ebfeaf6558c8c0dac)",
			  "(60019a95b6e67b47d7527ff814eeba1001261f54ce14684a14c3ab6f716bc934)",
			  "(000000aa76ae37c4085105a40d6eb27ef41ab5bed0013002cb6218b1e5fa6315)");
  }
  
  /**
  
  
  define SHA512 datafu.pig.hash.SHA('512');
  
  data_in = LOAD 'input' as (val:chararray);
  
  data_out = FOREACH data_in GENERATE SHA512(val) as val;
  
  STORE data_out INTO 'output';
   */
  @Multiline private String sha512Test;
  
  @Test
  public void sha512Test() throws Exception
  {
	  PigTest test = createPigTestFromString(sha512Test);
	  
	  writeLinesToFile("input", 
			  		   "ladsljkasdglk",
                       "lkadsljasgjskdjks",
                       "has_lo_md5_1065433",
                       "has_lo_sha512_92333");
	  
	  test.runScript();
	  
	  assertOutput(test, "data_out", 
			  "(f681dbd89dfc9edf00f68107ed81b4b7c89abdf84337921785d13d9189937a43decbc264b5013d396a102b18564c39595732c43d6d4cc99473f6d6d7101ecf87)",
			  "(85c130c8636c052e52a2ca091a92d0bb98ee361adcbeeebbd6af978a593b2486a22ac1e7352c683035cfa28de8eee3402adc6760ad54c5c7eda122c5124766bd)",
			  "(7d4f10c90ec6ca2112ae2f92c34e0d8ff557ed3fabdcef6b4b99af72194e6a60f9df311558f6556c04ba220b5c402c4dbb6268158762c6aa91e4e0a6ef13f8ec)",
			  "(000001f4836cd015a4b2205793484740b65a10626ef032dc8daec65bd369b05b7ecd7493fe263b187fcb356c3e02c05cf4aa5ebbd116305d1ec08d857135c8b2)");
  }
  
  /**
  

  define MD5 datafu.pig.hash.MD5('base64');
  
  data_in = LOAD 'input' as (val:chararray);
  
  data_out = FOREACH data_in GENERATE MD5(val) as val;
  
  STORE data_out INTO 'output';
   */
  @Multiline private String md5Base64Test;
  
  @Test
  public void md5Base64Test() throws Exception
  {
    PigTest test = createPigTestFromString(md5Base64Test);
    
    writeLinesToFile("input", 
                     "ladsljkasdglk",
                     "lkadsljasgjskdjks",
                     "aladlasdgjks",
                     "has_lo_md5_1065433");
            
    test.runScript();
        
    assertOutput(test, "data_out",
                 "(2agldXWLtJeJSdwGWSBcxg==)",
                 "(nsN/Avrg2Nan9EU6YicvHw==)",
                 "(y5QTmoufMkPmiomOxr2bPQ==)",
                 "(AAAI5Uh7Orrnvoih1LrVcw==)");
  }

  /**


  define MurmurH32   datafu.pig.hash.Hasher('murmur3-32');
  define MurmurH32A  datafu.pig.hash.Hasher('murmur3-32', '0x0');
  define MurmurH32B  datafu.pig.hash.Hasher('murmur3-32', '-1255333604');
  define MurmurH128  datafu.pig.hash.Hasher('murmur3-128');
  define MurmurH128A datafu.pig.hash.Hasher('murmur3-128', '0x0');
  define MurmurH128B datafu.pig.hash.Hasher('murmur3-128', '0x6705b75a');
  --
  define Sip24H      datafu.pig.hash.Hasher('sip24');
  define Sip24HA     datafu.pig.hash.Hasher('sip24', '000102030405060708090a0b0c0d0e0f');
  define Sip24HB     datafu.pig.hash.Hasher('sip24', '0f0e0d0c0b0a09080706050403020100');
  --
  define SHA1H       datafu.pig.hash.Hasher('sha1');
  define SHA256H     datafu.pig.hash.Hasher('sha256');
  define SHA512H     datafu.pig.hash.Hasher('sha512');
  --
  define MD5H        datafu.pig.hash.Hasher('md5');
  define CRC32       datafu.pig.hash.Hasher('crc32');
  define ADLER32     datafu.pig.hash.Hasher('adler32');

  data_in = LOAD 'input' as (val:chararray);

  most_hashes = FOREACH data_in GENERATE
    MurmurH32(val),  MurmurH32A(val),  MurmurH32B(val),
    MurmurH128(val), MurmurH128A(val), MurmurH128B(val),
    Sip24H(val),     Sip24HA(val),     Sip24HB(val),
    SHA1H(val),      SHA256H(val),     SHA512H(val),
    MD5H(val),       CRC32(val),       ADLER32(val)
    ;

  STORE most_hashes INTO 'output';
   */
  @Multiline private String hasherTest;

  @Test
  public void hasherTest() throws Exception
  {
    PigTest test = createPigTestFromString(hasherTest);

    writeLinesToFile("input",
                     "Of all who give and receive gifts, ",
                     "such as they are wisest. ",
                     "Everywhere they are wisest. They are the magi.",
                     "has_lo_md5_1065433");

    test.runScript();

    assertOutput(test, "most_hashes",
        "(5dd3faff,5dd3faff,5b07838d,70874abeba0d07a0848bde717968362e,70874abeba0d07a0848bde717968362e,0499c5014c999b641366cb43a5919b58,522e0f0ec7ec8e46,ad146f93da1f3cbe,4fda6b86417464dd,ce2df99fc66f35f8d00abbb3e58a73dcdc46857d,a6114a2f7bad5016cd11b0f74eab219bf629a41184041bb0e3dce16463a39674,73a7c303812965c115f9190ffdff843bf79289e643dc57d1d35865462fe6059daac2cd9209f50547ce63a902dc10f659aa0a4786e338a6e69062d3eeeccee252,b4b0d2cb8da053680556da959b611614,163c13a8,270c9ada)",
        "(f292b84a,f292b84a,9dd5afff,cf6c44df722abd72ccc7c485bd259ea3,cf6c44df722abd72ccc7c485bd259ea3,0c0cbaf8105ff62ed853f9105b35ccd8,fddd2aac3b10f452,23506548545203c0,a40b0cfc16c97d07,3db8d76faa5ae6b700db8f083e3162f1ff723edd,156acc8aa21b5110140bb5201245bdfaff99cab38f7a030dc6af8e198687e789,42ae14b43e2e4d75166dec84137c1c157c97a2fbf35e380f4f3b015a0af01a3f868b8ea1566a9b7564fbaef490b4e25614823e811ab43339c14a6d2c2fd0f5d0,8d0e66419d96d4a677f69758a7cf17cf,79710b14,e708e576)",
        "(820c3879,820c3879,59286b82,db96091e79c70a61dd328f92a17a657e,db96091e79c70a61dd328f92a17a657e,c6159ff8ed4c260820be3cee733e1fd7,4bd2a9cfde49ce76,b345eac42e6551ad,1ca9dc1084fd2ff7,c9f8b9ec0c9f92a898c81972304ea221ee3b87d3,6b6c1d0e17aa96a8d9dd616e62dc5c00147ba5167bdbaf204b3b3d2a424040fa,30d738452fbd9caaef06f6c7920a02a73797eb7644a361bdf53d154e4f9b2a8fc6a80dc8d3de09706191c76bd87666584fb0150b3c0e8e9a70bf318320771ae3,506a07d334a7fa034550d839671f17f2,348d70a4,a510ed94)",
        "(d3af58ed,d3af58ed,974db83c,301d20747aca51c7b73d4ceaf622b7fa,301d20747aca51c7b73d4ceaf622b7fa,c6c534e951734e55544e87f10dc155df,1c259c6488ad292f,9081ffb69dfeafc6,a87528ea5e3de55e,3d08148fe48f0e00486667833fd7b8bdc63412cf,60019a95b6e67b47d7527ff814eeba1001261f54ce14684a14c3ab6f716bc934,7d4f10c90ec6ca2112ae2f92c34e0d8ff557ed3fabdcef6b4b99af72194e6a60f9df311558f6556c04ba220b5c402c4dbb6268158762c6aa91e4e0a6ef13f8ec,000008e5487b3abae7be88a1d4bad573,5c7ff6ae,a1058d3d)");
  }

  /**

  define MurmurH32R  datafu.pig.hash.Hasher('murmur3-32',  'rand');
  define MurmurH128R datafu.pig.hash.Hasher('murmur3-128', 'rand');
  define Sip24HR     datafu.pig.hash.Hasher('sip24',       'rand');

  data_in = LOAD 'input' as (val:chararray);

  rand_hashes = FOREACH data_in GENERATE
    MurmurH32R(val), MurmurH128R(val), Sip24HR(val);

  STORE rand_hashes INTO 'output';

  define MurmurH32R  datafu.pig.hash.Hasher();
  define MurmurH128R datafu.pig.hash.Hasher();
  define Sip24HR     datafu.pig.hash.Hasher();
   */
  @Multiline private String hasherRandTest;

  @Test
  public void hasherRandTest() throws Exception
  {
    PigTest test = createPigTestFromString(hasherRandTest);
    // Stabilize the seed. Something weird is happening to cause the UDFs to be
    // instantiated over and over again, so changes to the methods in HashTests
    // may cause this test to fail.
    datafu.pig.hash.Hasher.randgen = new java.util.Random(69);

    writeLinesToFile("input",
                     "Of all who give and receive gifts, ",
                     "such as they are wisest. ",
                     "Everywhere they are wisest. They are the magi.");

    test.runScript();

    assertOutput(test, "rand_hashes",
        "(5b07838d,0499c5014c999b641366cb43a5919b58,2eb1957bdb2f3af6)",
        "(9dd5afff,0c0cbaf8105ff62ed853f9105b35ccd8,9cc583b6b68ac816)",
        "(59286b82,c6159ff8ed4c260820be3cee733e1fd7,8169775d40f01412)");
  }

  /**


  define DefaultH    datafu.pig.hash.Hasher();
  define GoodH       datafu.pig.hash.Hasher('good-32');
  define BetterH     datafu.pig.hash.Hasher('good-127');

  data_in = LOAD 'input' as (val:chararray);

  vals = FOREACH data_in GENERATE
    DefaultH(val) AS h_def,   GoodH(val) AS h_32,       BetterH(val) AS h_65;

  -- Seed value changes run-to-run, but we can at least ensure the bitness is sound
  good_hashes = FOREACH vals GENERATE
    ((h_def == h_32)     ? 'y' : 'n') AS default_to_32_bits,
    (SIZE(h_32)*4  >= 32 ? 'y' : 'n') AS got_at_least_32_bits,
    (SIZE(h_65)*4  >= 65 ? 'y' : 'n') AS got_at_least_65_bits;

  STORE good_hashes INTO 'output';
   */
  @Multiline private String hasherGoodTest;

  @Test
  public void hasherGoodTest() throws Exception
  {
    PigTest test = createPigTestFromString(hasherGoodTest);

    writeLinesToFile("input",
                     "Of all who give and receive gifts, ",
                     "such as they are wisest. ",
                     "Everywhere they are wisest. They are the magi.");

    test.runScript();

    assertOutput(test, "good_hashes",
      "(y,y,y)",
      "(y,y,y)",
      "(y,y,y)");
  }
}
