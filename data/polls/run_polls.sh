for i in `seq 1 10`
do
    echo `pwd`
    survey="poll$i.csv"
    java -jar ../../runner.jar --backend=MTURK --breakoff=false $survey &
done
