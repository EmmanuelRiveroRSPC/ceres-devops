import yaml
import argparse
import subprocess

def runPython(testName, arguments):
    cmd = ["python", testName, arguments]
    print (cmd)

def runJava(testName, arguments):
    cmd = ["java", "-jar", testName, arguments]
    print (cmd)

suportedTypes = ['python', 'java']

parser=argparse.ArgumentParser()

parser.add_argument('-l', '--list', help='Test list file path', default="." )
parser.add_argument('-t', '--test_set', help='Test set to be run', required=True)

args=parser.parse_args()

listPath = args.list
testSet = args.test_set

alltestSet = {}
with open(listPath) as file:
    # The FullLoader parameter handles the conversion from YAML
    # scalar values to Python the dictionary format
    alltestSet = yaml.load(file, Loader=yaml.FullLoader)

for test in alltestSet[testSet]:
    if test["type"] == "python":
        runPython(testName=test["name"], arguments=test["argLine"])

    if test["type"] == "java":
        print("tyolo")
    
    if test["type"] not in suportedTypes:
        print ("Type: {} not suported for test: {}".format(test["type"], test["name"]))
        print ("Suported types:")
        for appType in suportedTypes:
            print (f"  {appType}")
    
    