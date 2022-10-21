echo 'This code will train and test the trust-prediction and citeseer datasets in PSL and SRLBoost' 
echo ''

echo Running trust-prediction on PSL
bash PSL/trust-prediction/cli/run.sh > logs/trust-prediction_psl.txt

echo Running citeseer on PSL
bash PSL/citeseer/cli/run.sh > logs/citeseer_psl.txt

echo Training trust-prediction on SRLBoost
java -jar SRLBoost/build/libs/srlboost-0.3.0.jar -l -train SRLBoost/data/trust/train/ -target trusts > logs/trust-prediction-train-srlboost.txt

echo Running trust-prediction on SRLBoost
java -jar SRLBoost/build/libs/srlboost-0.3.0.jar -i -model SRLBoost/data/trust/train/models/ -test SRLBoost/data/trust/test/ -target trusts > logs/trust-prediction-srlboost.txt

echo Training citeseer on SRLBoost
java -jar SRLBoost/build/libs/srlboost-0.3.0.jar -l -train SRLBoost/data/citeseer/train/ -target hasCat > logs/citeseer-train-srlboost.txt

echo Running citeseer on SRLBoost
java -jar SRLBoost/build/libs/srlboost-0.3.0.jar -i -model SRLBoost/data/citeseer/train/models/ -test SRLBoost/data/citeseer/test/ -target hasCat > logs/citeseer-srlboost.txt
