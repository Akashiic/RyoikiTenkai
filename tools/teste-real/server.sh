#!/bin/bash
cd ${SDIR:-/home/user/teof/server}
rm -f in.fifo; mkfifo in.fifo
nohup bash -c 'tail -f in.fifo | java -Xms1G -Xmx${SXMX:-5G} -Dfml.queryResult=confirm @java9args.txt -javaagent:Akashic-nChat-2.0.4-ChainColor-0.5.1-agent.jar -javaagent:Akashic-RGB-Transport-1.1.1-rc.6-agent.jar -jar server.jar nogui' > console.log 2>&1 &
