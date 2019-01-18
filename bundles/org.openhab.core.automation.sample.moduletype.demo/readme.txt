# Eclipse SmartHome Automation Module Type Demo #

## Description ##

The purpose of the demo is to give an example, of how to define a module types in .json files, how to create a handlers for them
and how to register a ModuleHandlerFactory that creates handlers for the defined module types.

Following module types are provided:
CompareCondition - it can perform a basic comparison on integer values.

ConsolePrintAction - it prints it input to the standard output

ConsoleTrigger - it is triggered from org.osgi.service.event.Event.

Example rule definition:

[  
   {  
      "uid":"DemoRule",
      "name":"Demo Rule",
      "triggers":[  
         {  
            "id":"RuleTrigger",
            "type":"ConsoleTrigger",
            "configuration":{  
               "eventTopic":"demo/topic",
               "keyName":"key"
            }
         }
      ],
      "conditions":[  
         {  
            "id":"RuleCondition",
            "type":"CompareCondition",
            "configuration":{  
               "operator":"=",
               "constraint":5
            },
            "inputs":{  
               "inputValue":"RuleTrigger.outputValue"
            }
         }
      ],
      "actions":[  
         {  
            "id":"RuleAction",
            "type":"ConsolePrintAction",
            "inputs":{  
               "inputValue":"RuleTrigger.outputValue"
            }
         }
      ]
   }
] 

To trigger this rule and satisfy rule's condition you need to type in the console:
atmdemo postEvent demo/topic key 5