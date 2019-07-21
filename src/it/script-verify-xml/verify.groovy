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

// from http://groovy-lang.org/processing-xml.html
// A lot of examples covering xml assertions 

class XmlExamples {
  static def CAR_RECORDS = '''
    <records>
      <car name='HSV Maloo' make='Holden' year='2006'>
        <country>Australia</country>
        <record type='speed'>Production Pickup Truck with speed of 271kph</record>
      </car>
      <car name='P50' make='Peel' year='1962'>
        <country>Isle of Man</country>
        <record type='size'>Smallest Street-Legal Car at 99cm wide and 59 kg in weight</record>
      </car>
      <car name='Royale' make='Bugatti' year='1931'>
        <country>France</country>
        <record type='price'>Most Valuable Car at $15 million</record>
      </car>
    </records>
  '''
}
def records = new XmlSlurper().parseText(XmlExamples.CAR_RECORDS)
def allRecords = records.car
assert 3 == allRecords.size()
def allNodes = records.depthFirst().collect{ it }
assert 10 == allNodes.size()
def firstRecord = records.car[0]
assert 'car' == firstRecord.name()
assert 'Holden' == firstRecord.@make.text()
assert 'Australia' == firstRecord.country.text()
def carsWith_e_InMake = records.car.findAll{ it.@make.text().contains('e') }
assert carsWith_e_InMake.size() == 2
// alternative way to find cars with 'e' in make
assert 2 == records.car.findAll{ it.@make =~ '.*e.*' }.size()
// makes of cars that have an 's' followed by an 'a' in the country
assert ['Holden', 'Peel'] == records.car.findAll{ it.country =~ '.*s.*a.*' }.@make.collect{ it.text() }
def expectedRecordTypes = ['speed', 'size', 'price']
assert expectedRecordTypes == records.depthFirst().grep{ it.@type != '' }.'@type'*.text()
assert expectedRecordTypes == records.'**'.grep{ it.@type != '' }.'@type'*.text()
def countryOne = records.car[1].country
assert 'Peel' == countryOne.parent().@make.text()
assert 'Peel' == countryOne.'..'.@make.text()
// names of cars with records sorted by year
def sortedNames = records.car.list().sort{ it.@year.toInteger() }.'@name'*.text()
assert ['Royale', 'P50', 'HSV Maloo'] == sortedNames
assert ['Australia', 'Isle of Man'] == records.'**'.grep{ it.@type =~ 's.*' }*.parent().country*.text()
assert 'co-re-co-re-co-re' == records.car.children().collect{ it.name()[0..1] }.join('-')
assert 'co-re-co-re-co-re' == records.car.'*'.collect{ it.name()[0..1] }.join('-')
