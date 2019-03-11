## Prerequisites
* sbt
* java
* docker and docker-compose
* git

## Steps
```bash 
git clone https://github.com/youngce/akka-auto-scaling-sample.git 
cd akka-auto-scaling-sample

# building a docker image
sbt docker:publishLocal 
# set-up akka cluster services
docker-compose up
# scaling node to 10
docker-compose scale node=10 

```