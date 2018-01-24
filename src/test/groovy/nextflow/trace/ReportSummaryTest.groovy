/*
 * Copyright (c) 2013-2018, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2018, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.trace

import spock.lang.Specification
import spock.lang.Unroll
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ReportSummaryTest extends Specification {

    private double round ( double value ) {
        Math.round(value * 100 ) / 100
    }

    private quantile( List items, int q0 ) {
        def stats = new ReportSummary.Summary({ it as Double })
        round(stats.quantile(items.sort(), q0))
    }

    private mean( List items ) {
        round(items.sum() / items.size())
    }

    def 'should get report summary stats' () {
        given:
        def CPU = [10,77,33,98,12,87,23,12,76,21]
        def MEM = [65,34,23,65,11,54,78,87,32,21]
        def TIME = [22,21,76,78,07,32,99,32,11,12]
        def READS = [87,32,65,87,23,11,19,87,29,33]
        def WRITES = [83,88,99,32,65,89,32,87,87,43]
        def data = [
            '%cpu': CPU,
            'vmem': MEM,
            realtime: TIME,
            'read_bytes': READS,
            'write_bytes': WRITES,
        ]

        def report = new ReportSummary()

        10.times { int index ->
            def t=new TraceRecord([process:'foo', name:"foo-$index"]);
            data.each { name, col ->  t.put(name, col[index])}
            // add a new record
            report.add(t)
        }


        when:
        def cpu = report.compute('cpu')
        def mem = report.compute('mem')
        def time = report.compute('time')
        def reads = report.compute('reads')
        def writes = report.compute('writes')
        then:

        cpu.min == quantile(CPU, 0)
        cpu.q1 == quantile(CPU, 25)
        cpu.q2 == quantile(CPU, 50)
        cpu.q3 == quantile(CPU, 75)
        cpu.max == quantile(CPU, 100)
        cpu.mean == mean(CPU)
        cpu.minLabel == 'foo-0'
        cpu.maxLabel == 'foo-3'
        cpu.q1Label == 'foo-7'
        cpu.q2Label == 'foo-6'
        cpu.q3Label == 'foo-8'

        mem.min == quantile(MEM, 0)
        mem.q1 == quantile(MEM, 25)
        mem.q2 == quantile(MEM, 50)
        mem.q3 == quantile(MEM, 75)
        mem.max == quantile(MEM, 100)
        mem.mean == mean(MEM)

        time.min == quantile(TIME, 0)
        time.q1 == quantile(TIME, 25)
        time.q2 == quantile(TIME, 50)
        time.q3 == quantile(TIME, 75)
        time.max == quantile(TIME, 100)
        time.mean == mean(TIME)

        reads.min == quantile(READS, 0)
        reads.q1 == quantile(READS, 25)
        reads.q2 == quantile(READS, 50)
        reads.q3 == quantile(READS, 75)
        reads.max == quantile(READS, 100)
        reads.mean == mean(READS)

        writes.min == quantile(WRITES, 0)
        writes.q1 == quantile(WRITES, 25)
        writes.q2 == quantile(WRITES, 50)
        writes.q3 == quantile(WRITES, 75)
        writes.max == quantile(WRITES, 100)
        writes.mean == mean(WRITES)
    }


    @Unroll
    def 'should calc quantiles' () {

        given:
        def stats = new ReportSummary.Summary({ it as Double })

        expect:
        stats.quantiles(NUMBERS.sort()) == EXPECTED

        where:
        EXPECTED                        | NUMBERS
        [1, 3.5, 6, 8.5, 11]            |  [1 , 3 , 5 , 7 , 9, 11]
        [7, 20.25, 37.50, 39.75, 41]    |  [7, 15, 36, 39, 40, 41 ]
        [10, 12, 15, 19.50, 26]         |  [10, 11, 13, 15, 16, 23, 26]
        [3, 7, 12, 14, 21 ]             |  [3, 5, 7, 8, 12, 13, 14, 18, 21]
        [6, 25.50, 40, 42.50, 49]       |  [6, 7, 15, 36, 39, 40, 41, 42, 43, 47, 49]
        [7, 20.25, 37.50, 39.75, 41]    |  [7, 15, 36, 39, 40, 41]
        [10, 12.50, 15.50, 23.75, 32]   |  [10, 11, 13, 15, 16, 23, 26, 32]
        [11, 244, 605, 787, 998]        |  [11,99,104,107,112,153,176,177,180,185,188,189,244,255,296,312,340,361,479,531,536,560,568,581,605,642,654,671,693,700,709,710,728,777,778,783,787,790,795,806,812,816,832,861,873,878,905,939,998]
        [7, 202.75, 593, 786, 998]      |  [7,11,99,104,107,112,153,176,177,180,185,188,189,244,255,296,312,340,361,479,531,536,560,568,581,605,642,654,671,693,700,709,710,728,777,778,783,787,790,795,806,812,816,832,861,873,878,905,939,998]

    }

    def 'should set q1,q2,q3 labels' () {
        given:
        def records = []
        [3, 5, 7, 8, 12, 13, 14, 18, 21].eachWithIndex { v, i ->
            records << new TraceRecord([realtime: v, name: "task-${i+1}"])
        }

        def mapping = { TraceRecord r -> r.get('realtime') as Double }
        def stats = new ReportSummary.Summary(mapping)
        records.sort(mapping)

        expect:
        stats.quantile(records, 0) == 3
        stats.quantile(records, 25) == 7
        stats.quantile(records, 50) == 12
        stats.quantile(records, 75) == 14
        stats.quantile(records, 100) == 21

        stats.minLabel == 'task-1'
        stats.q1Label == 'task-3'
        stats.q2Label == 'task-5'
        stats.q3Label == 'task-7'
        stats.maxLabel == 'task-9'

    }



}
