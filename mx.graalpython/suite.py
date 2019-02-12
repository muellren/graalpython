suite = {
    # --------------------------------------------------------------------------------------------------------------
    #
    #  METADATA
    #
    # --------------------------------------------------------------------------------------------------------------
    "mxversion": "5.190.1",
    "name": "graalpython",
    "versionConflictResolution": "latest",

    "version": "1.0.0-rc13",
    "release": False,
    "groupId": "org.graalvm.graalpython",
    "url": "http://www.graalvm.org/",

    "developer": {
        "name": "Truffle and Graal developers",
        "email": "graalvm-users@oss.oracle.com",
        "organization": "Graal",
        "organizationUrl": "http://www.graalvm.org/",
    },

    "scm": {
        "url": "https://github.com/graalvm/graalpython",
        "read": "https://github.com/graalvm/graalpython.git",
        "write": "git@github.com:graalvm/graalpython.git",
    },

    # --------------------------------------------------------------------------------------------------------------
    #
    #  DEPENDENCIES
    #
    # --------------------------------------------------------------------------------------------------------------
    "imports": {
        "suites": [
            {
                "name": "truffle",
                "versionFrom": "sulong",
                "subdir": True,
                "urls": [
                    {"url": "https://github.com/oracle/graal", "kind": "git"},
                ]
            },
            {
                "name": "sulong",
                "version": "175738c97f96974ba44251e913a597aa64a7d3c2",
                "subdir": True,
                "urls": [
                    {"url": "https://github.com/oracle/graal", "kind": "git"},
                ]
            },
            {
                "name": "regex",
                "version": "175738c97f96974ba44251e913a597aa64a7d3c2",
                "subdir": True,
                "urls": [
                    {"url": "https://github.com/oracle/graal", "kind": "git"},
                ]
            },
        ],
    },

    # --------------------------------------------------------------------------------------------------------------
    #
    #  REPOS
    #
    # --------------------------------------------------------------------------------------------------------------
    "repositories": {
        "python-public-snapshots": {
            "url": "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
            "licenses": ["GPLv2-CPE", "UPL", "BSD-new", "MIT"]
        },
        "python-local-snapshots": {
            "url": "http://localhost",
            "licenses": ["GPLv2-CPE", "UPL", "BSD-new", "MIT"]
        },
    },

    "defaultLicense": "UPL",

    # --------------------------------------------------------------------------------------------------------------
    #
    #  LIBRARIES
    #
    # --------------------------------------------------------------------------------------------------------------
    "libraries": {
        "SETUPTOOLS": {
            "urls": [
                "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/setuptools-40.6.3.zip",
            ],
            "sha1": "7a5960b8062ddbf0c0e79f806e23785d55fec3c8",
        }
    },

    # --------------------------------------------------------------------------------------------------------------
    #
    #  PROJECTS
    #
    # --------------------------------------------------------------------------------------------------------------
    "externalProjects": {
        "lib.python": {
            "type": "python",
            "path": 'graalpython/lib-python',
            "source": [
                "3"
            ]
        },

        "lib.graalpython": {
            "type": "python",
            "path": 'graalpython/lib-graalpython',
            "source": []
        },

        "perf.benchmarks": {
            "type": "python",
            "path": 'graalpython/benchmarks',
            "source": [
                "src"
            ]
        },

        "util.scripts": {
            "type": "python",
            "path": 'scripts',
            "source": []
        },

        "com.oracle.graal.python.cext": {
            "type": "python",
            "path": "graalpython/com.oracle.graal.python.cext",
            "source": [
                "include",
                "src",
                "modules"
            ]
        },
    },


    "projects": {
        # GRAALPYTHON ANTLR
        "com.oracle.graal.python.parser.antlr": {
            "subDir": "graalpython",
            "buildEnv": {
                "ANTLR_JAR": "<path:truffle:ANTLR4_COMPLETE>",
                "PARSER_PATH": "<src_dir:com.oracle.graal.python>/com/oracle/graal/python/parser/antlr",
                "OUTPUT_PATH": "<src_dir:com.oracle.graal.python>/com/oracle/graal/python/parser/antlr",
                "PARSER_PKG": "com.oracle.graal.python.parser.antlr",
                "POSTPROCESSOR": "<suite:graalpython>/graalpython/com.oracle.graal.python.parser.antlr/postprocess.py",
            },
            "dependencies": [
                "truffle:ANTLR4_COMPLETE",
            ],
            "native": True,
            "vpath": True,
        },

        "com.oracle.graal.python.shell": {
            "subDir": "graalpython",
            "sourceDirs": ["src"],
            "dependencies": [
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "javaCompliance": "1.8",
        },

        # GRAALPYTHON
        "com.oracle.graal.python": {
            "subDir": "graalpython",
            "sourceDirs": ["src"],
            "dependencies": [
                "truffle:TRUFFLE_API",
                "sdk:GRAAL_SDK",
                "truffle:ANTLR4",
                "regex:TREGEX",
            ],
            "buildDependencies": ["com.oracle.graal.python.parser.antlr"],
            "checkstyle": "com.oracle.graal.python",
            "javaCompliance": "1.8",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "Truffle,Python",
        },

        # GRAALPYTHON TEST
        "com.oracle.graal.python.test": {
            "subDir": "graalpython",
            "sourceDirs": ["src"],
            "dependencies": [
                "com.oracle.graal.python.shell",
                "com.oracle.graal.python",
                "truffle:TRUFFLE_TCK",
                "mx:JUNIT"
            ],
            "checkstyle": "com.oracle.graal.python",
            "javaCompliance": "1.8",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "Truffle,Python",
            "testProject": True,
        },

        "com.oracle.graal.python.tck": {
            "subDir": "graalpython",
            "sourceDirs": ["src"],
            "dependencies": [
                "sdk:POLYGLOT_TCK",
                "mx:JUNIT"
            ],
            "checkstyle": "com.oracle.graal.python",
            "javaCompliance": "1.8",
            "workingSets": "Truffle,Python",
            "testProject": True,
        },


        "com.oracle.graal.python.cext": {
            "subDir": "graalpython",
            "native": True,
            "vpath": True,
            "results": ["graalpython/lib-graalpython"],
            "output": ".",
            "buildDependencies": [
                "sulong:SULONG_LIBS",
            ],
            "buildEnv": {
                "POLYGLOT_INC": "<path:SULONG_LIBS>",
            },
        },

        "python-lib": {
            "class": "ArchiveProject",
            "outputDir": "graalpython/lib-python/3",
            "prefix": "lib-python/3",
            "ignorePatterns": [
                "\/test\/",
                "\/tests\/",
                "\/idle_test\/",
            ],
            "license": ["PSF-License"],
        },

        "site-packages": {
            "subDir": "graalpython",
            "native": True,
            "vpath": False,
            "output": ".",
            "license": ["MIT"],
            "defaultBuild": False,
            "buildEnv": {
                "SETUPTOOLS_ZIP": "<path:SETUPTOOLS>",
            },
            "buildDependencies": [
                "SETUPTOOLS",
                "GRAALPYTHON-LAUNCHER",
                "GRAALPYTHON",
            ],
        },

        "com.nvidia.rapids.gpulang": {
            "subDir": "gpulang",
            "output": "gpulang/output",
            "license": ["Apache-2.0"],
            "sourceDirs": ["src"],
            "javaCompliance": "1.8",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "dependencies": [
                "truffle:TRUFFLE_API",
                "sdk:GRAAL_SDK",
            ]
        },
    },

    "licenses": {
        "PSF-License": {
            "name": "Python Software Foundation License",
            "url": "https://docs.python.org/3/license.html",
        },
        "UPL": {
            "name": "Universal Permissive License, Version 1.0",
            "url": "http://opensource.org/licenses/UPL",
        },
    },

    # --------------------------------------------------------------------------------------------------------------
    #
    #  DISTRIBUTIONS
    #
    # --------------------------------------------------------------------------------------------------------------
    "distributions": {
        "GRAALPYTHON-LAUNCHER": {
            "dependencies": [
                "com.oracle.graal.python.shell",
            ],
            "distDependencies": [
                "sdk:GRAAL_SDK",
                "sdk:LAUNCHER_COMMON",
            ],
            "description": "GraalPython launcher",
        },

        "GRAALPYTHON": {
            "dependencies": [
                "com.nvidia.rapids.gpulang",
                "com.oracle.graal.python",
            ],
            "distDependencies": [
                "GRAALPYTHON-LAUNCHER",
                "truffle:TRUFFLE_API",
                "regex:TREGEX",
                "sdk:GRAAL_SDK",
                "truffle:ANTLR4",
            ],
            "sourcesPath": "graalpython.src.zip",
            "description": "GraalPython engine",
        },

        "GRAALPYTHON_PYTHON_LIB": {
            "dependencies": ["python-lib"],
            "description": "Python 3 lib files",
            "maven": False,
        },

        "GRAALPYTHON_UNIT_TESTS": {
            "description": "unit tests",
            "dependencies": [
                "com.oracle.graal.python.test",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": [
                "GRAALPYTHON",
                "GRAALPYTHON-LAUNCHER",
                "truffle:TRUFFLE_TCK",
            ],
            "sourcesPath": "graalpython.tests.src.zip",
            "testDistribution": True,
        },

        "GRAALPYTHON_TCK": {
            "description": "unit tests",
            "dependencies": [
                "com.oracle.graal.python.tck",
            ],
            "exclude": ["mx:JUNIT"],
            "distDependencies": [
                "sdk:POLYGLOT_TCK",
            ],
            "sourcesPath": "graalpython.tests.src.zip",
            "testDistribution": True,
        },

        "GRAALPYTHON_GRAALVM_SUPPORT": {
            "native": True,
            "platformDependent": True,
            "description": "Graal.Python support distribution for the GraalVM",
            "layout": {
                "./": [
                    "dependency:com.oracle.graal.python.cext/graalpython/lib-graalpython",
                    "file:graalpython/com.oracle.graal.python.cext/include",
                    "extracted-dependency:graalpython:GRAALPYTHON_PYTHON_LIB",
                    "file:mx.graalpython/native-image.properties",
                ],
                "LICENSE_GRAALPYTHON": "file:LICENSE",
                "3rd_party_licenses_graalpython.txt": "file:3rd_party_licenses.txt",
            },
            "maven": False,
        },

        "GRAALPYTHON_GRAALVM_DOCS": {
            "native": True,
            "description": "Graal.Python documentation files for the GraalVM",
            "layout": {
                "README_GRAALPYTHON.md": "file:README.md",
                "./": "file:doc",
            },
            "maven": False,
        },
    },
}
