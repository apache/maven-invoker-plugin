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

String[] expected = [
        "target/its/script-ret-true/target/touch.txt",
        "target/its/script-ret-quiet/target/touch.txt",
  ];
for ( String file : expected )
{
    File touchFile = new File( basedir, file );
    System.out.println( "Checking for existence of: " + touchFile );
    assert touchFile.isFile()
}

String[] unexpected = [
        "target/its/script-ret-false/target/touch.txt",
        "target/its/script-ret-other/target/touch.txt",
        "target/its/script-throws/target/touch.txt",
  ];
for ( String file : unexpected )
{
    File touchFile = new File( basedir, file );
    System.out.println( "Checking for absence of: " + touchFile );
    assert !touchFile.exists() : file  + " should not be here -> FAILED!"
}

String[] success = [
        "target/invoker-reports-test/BUILD-script-ret-true.xml",
        "target/invoker-reports-test/BUILD-script-ret-quiet.xml",
  ];
for ( String file : success )
{
    File resultFile = new File( basedir, file );
    System.out.println( "Checking build result: " + resultFile );
    String xml = resultFile.getText( 'UTF-8' );
    assert xml.indexOf("result=\"success\"") >= 0 || xml.indexOf("result='success'") >= 0 : resultFile + " not success -> FAILED!"
}

String[] skipped = [
        "target/invoker-reports-test/BUILD-script-ret-false.xml",
        "target/invoker-reports-test/BUILD-script-ret-other.xml",
  ];
for ( String file : skipped )
{
    File resultFile = new File( basedir, file );
    System.out.println( "Checking build result: " + resultFile );
    String xml = resultFile.getText( 'UTF-8' );
    assert xml.indexOf("result=\"skipped\"") >= 0 || xml.indexOf("result='skipped'") >= 0 : resultFile + " not skipped -> FAILED!"
}

String[] inerror = [
        "target/invoker-reports-test/BUILD-script-throw.xml",
  ];
for ( String file : inerror )
{
    File resultFile = new File( basedir, file );
    System.out.println( "Checking build result: " + resultFile );
    String xml = resultFile.getText( 'UTF-8' );
    assert xml.indexOf("result=\"error\"") >= 0 || xml.indexOf("result='error'") >= 0 : resultFile + " not failed -> FAILED!"
}
