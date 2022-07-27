# test run script
# Will run a test set in test-plan yaml file
# Will try to load test scripts from local, other wise will try to download from artifactory
# Each test in the test set must have a name, type(python, java...), and argLine(arguments)
# Artifactory url and repository must be specified in the test-plan yaml

from genericpath import isfile
from syslog import LOG_INFO
import yaml
import argparse
import subprocess
import os
import requests

#Coloritos!!!!
class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    RESETALL = '\033[0m'

### Sum a list of logicial expresions 
def logocialSum(logicals):
    result = True
    for i in logicals:
        result = result and i
    return result

def runProcess(cmd):
    Process = subprocess.run(cmd, stdout=subprocess.PIPE)
    Text = Process.stdout.decode('utf-8')
    print (Text)
    
    if Process.returncode == 0:
        return True
    else:
        return False

def removeLastSlash(string):
    length = len(string)
    if string[length - 1] == "/":
        return string[:-1]
    else:
        return string

#Check if the file exist in local, if not downlod it from artifactory 
def fileCheck(localFile, fileName, type):
    url = removeLastSlash(Artifactory.url)

    if not os.path.isfile(localFile):
        print ("File not in local path...")
        if not Artifactory.skip:
            print ("Downloading from Artifactory")
            ArtifactoryURL = url + "/" + Artifactory.repository + "/" + type + "/" + fileName
            response = requests.get(ArtifactoryURL)

            if response.status_code == 200:
                open(localFile, "wb").write(response.content)
            else: 
                print ("can't download the test file")
                return False
        else:
            return False
    return True

def runTest(testName, arguments, testDirectory, testType):
    fileName = ""
    cmd = []
    if testType == "python" or testType == "py":
        cmd = ["python"]
        fileName = testName + ".py"
    if testType == "java" or testType =="jar":
        cmd = ["java", "-jar"]
        fileName = testName + ".jar"
    if testType == "ruby" or testType == "rb":
        cmd = ["java", "-jar"]
        fileName = testName + ".rb"

    if testType not in suportedTypes.supported:
        print ("{}: {} type not suported for test".format(testName, testType))
        print ("Suported types:")
        for appType in suportedTypes.supported:
            print (f"  {appType}")
        return {"name":testName, "result":"Not Supported"}

    localFile = f"{testDirectory}/{fileName}"

    if not fileCheck(localFile, fileName, testType):
        return {"name":testName, "result":"Not Found"}

    cmd.append(localFile)
    
    argumentsList = arguments.split()
    cmd.extend(argumentsList)
    if runProcess(cmd):
        result = "Pass"
    else:
        result = "Failed"
    
    return {"name":testName, "result":result}

class Artifactory:
    url = ""
    repository = ""
    skip = True

class suportedTypes:
    supported = ['python', 'java', 'ruby', 'py', 'jar', 'rb']

def main():
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

    if "Artifactory" in alltestSet:
        Artifactory.url = alltestSet["Artifactory"]["url"]
        Artifactory.repository = alltestSet["Artifactory"]["repository"]
        Artifactory.skip = False

    SummaryList = []

    for test in alltestSet[testSet]:
        SummaryList.append(runTest(testName=test["name"], arguments=test["argsLine"], testDirectory=testDirectory, testType=test["type"]))
    
    outcome = []
    print ("Test run completed")
    print (bcolors.HEADER + bcolors.BOLD + "Summary:" + bcolors.RESETALL)

    for test in SummaryList:
        printable = ""
        if test["result"] == "Pass":
            printable = bcolors.OKGREEN + test["result"]
            outcome.append(True)
        else:
            printable = bcolors.FAIL + test["result"]
            outcome.append(False)

        print (" {}: {}{}".format(test["name"], printable, bcolors.RESETALL))
    
    if not logocialSum(outcome):
        exit(1)

if __name__ == "__main__":
    main()