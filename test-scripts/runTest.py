from genericpath import isfile
import yaml
import argparse
import subprocess
import os

def runPython(testName, arguments, testDirectory):
    localPath = f"{testDirectory}/{testName}.py"

    if not os.path.isfile(localPath):
        print ("not in local")

    cmd = ["python", localPath]

    argumentsList = arguments.split()
    cmd.extend(argumentsList)
    Process = subprocess.run(cmd, stdout=subprocess.PIPE)
    Text = Process.stdout.decode('utf-8')
    print (Text)
    
    if Process.returncode == 0:
        return True
    else:
        return False

def runJava(testName, arguments):
    cmd = ["java", "-jar", testName]

    argumentsList = arguments.split()
    cmd.extend(argumentsList)

    print (cmd)

suportedTypes = ['python', 'java']

parser=argparse.ArgumentParser()

parser.add_argument('-l', '--list', help='Test list file path', default="./test-plan.yml" )
parser.add_argument('-t', '--test_set', help='Test set to be run', required=True)
parser.add_argument('-d', '--test_directory', help='local test directory', default=".")

args=parser.parse_args()

listPath = args.list
testSet = args.test_set
testDirectory=args.test_directory

alltestSet = {}
with open(listPath) as file:
    # The FullLoader parameter handles the conversion from YAML
    # scalar values to Python the dictionary format
    alltestSet = yaml.load(file, Loader=yaml.FullLoader)

SummaryList = []

for test in alltestSet[testSet]:
    if test["type"] == "python":
        print ("Starting test: {}".format(test["name"]) )
        result = ""
        if runPython(testName=test["name"], arguments=test["argsLine"], testDirectory=testDirectory):
            result = "Pass"
        else:
            result = "Failed"
        SummaryList.append({"name":test["name"], "result":result})

    if test["type"] == "java":
        runJava(testName=test["name"], arguments=test["argsLine"])
    
    if test["type"] not in suportedTypes:
        print ("Type: {} not suported for test: {}".format(test["type"], test["name"]))
        print ("Suported types:")
        for appType in suportedTypes:
            print (f"  {appType}")
    
print ("All test complete")
print ("Summary:")
for test in SummaryList:
    print ("{}:{}".format(test["name"], test["result"]))