## Build

The Alternet Libs Web site (http://alternet.ml/alternet-libs/) is built and deployed to Github with Maven

---

### Directory structure

Location of the Alternet Libs projects :

```
[workspace]/ml.alternet/alternet-libs/pom.xml
[workspace]/ml.alternet/scanner/pom.xml
[workspace]/ml.alternet/security/pom.xml
[workspace]/ml.alternet/tools/pom.xml
[workspace]/ml.alternet/[...]/pom.xml
```

Location of the web site after maven build (the web site aggregates all the modules) :

```
[workspace]/ml.alternet/alternet-libs/target/staging/
```

Location of the local Github repo :

```
[workspace]/alternet/alternet.github.io/alternet-libs/
```

### Maven commands

The required Maven commands can't be performed at once, since every module will be involved, starting with the
parent project (alternet-libs), whereas publishing the site MUST be done at the very end (and is also driven by
the parent project).

* Step 1 : build the local sites and run the aggregation :
(run test before site since some reports are aggregated)

```
cd [workspace]/ml.alternet/
./mvn-site.sh
```

(clean alternet-libs, clean install site all subprojects, install site alternet-libs)

Tests failures are ignored.

* Step 2 : check the site in the staging directory

* Step 3 : deploy to Github :

```
mvn -N site-deploy -Psite-deploy
```

The -N switch disables running Maven recursively on the submodules.

The -Psite-deploy profile disables to previously "site" building, copy the site to the local Github repo, and
then push it to the Github server.

### Manual operations

#### Upload

From `[workspace]/alternet/alternet.github.io/`

```
rm -R alternet-libs/
cp -R ../../ml.alternet/alternet-libs/target/staging/ alternet-libs

git add --all
git commit -m "Update Website"
git push -u origin master
```

### Issues

A first attempt with 2 separate profiles (the former for the maven build/report, the latter for Github) had failed :

```
[ERROR] Parser Exception: META-INF/maven/site.vm
[ERROR] org.apache.velocity.runtime.parser.ParseException: Encountered "#end\n" at line 2, column 11.
```

The built-in velocity template seems to dislike profiles (maybe with the skin used), this is why there is a
"normal" run and a "profiled" run, the latter disabling the configuration of the former.