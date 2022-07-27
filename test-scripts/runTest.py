from genericpath import isfile
import yaml
import argparse
import subprocess
import os

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

def runProcess(cmd):
    Process = subprocess.run(cmd, stdout=subprocess.PIPE)
    Text = Process.stdout.decode('utf-8')
    print (Text)
    
    if Process.returncode == 0:
        return True
    else:
        return False

def runTest(testName, arguments, testDirectory, testType):
    localPath = f"{testDirectory}/{testName}.py"

    if not os.path.isfile(localPath):
        print ("not in local")

    cmd = []
    if testType == "python":
        cmd = ["python"]
    if testType == "java":
        cmd = ["java", "-jar"]

    if testType not in suportedTypes.suppoerted:
        print ("Type: {} not suported for test: {}".format(test["type"], test["name"]))
        print ("Suported types:")
        for appType in suportedTypes.suppoerted:
            print (f"  {appType}")
        exit(1)

    cmd.append(localPath)
    
    argumentsList = arguments.split()
    cmd.extend(argumentsList)
    if runProcess(cmd):
        result = "Pass"
    else:
        result = "Failed"
    
    return {"name":test["name"], "result":result}

class Artifactory:
    url = ""
    repository = ""
    skip = True

class suportedTypes:
    suppoerted =  ['python', 'java']

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
        
    print ("All test complete")
    print (bcolors.HEADER + bcolors.BOLD + "Summary:" + bcolors.RESETALL)
    for test in SummaryList:
        printable = ""
        if test["result"] == "Pass":
            printable = bcolors.OKGREEN + test["result"]
        else:
            printable = bcolors.FAIL + test["result"]
        print (" {}: {}{}".format(test["name"], printable, bcolors.RESETALL))

if __name__ == "__main__":
    main()