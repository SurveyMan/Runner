for i in `seq 1 1`
do
    echo `pwd`
    survey="data/polls/poll$i.csv"
    java -jar runner.jar --backend=MTURK --properties=./.surveyman/params.properties $survey &
done
