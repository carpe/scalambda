// When the user clicks on the search box, we want to toggle the search dropdown
function displayToggleSearch(e) {
  e.preventDefault();
  e.stopPropagation();

  closeDropdownSearch(e);
  
  if (idx === null) {
    console.log("Building search index...");
    prepareIdxAndDocMap();
    console.log("Search index built.");
  }
  const dropdown = document.querySelector("#search-dropdown-content");
  if (dropdown) {
    if (!dropdown.classList.contains("show")) {
      dropdown.classList.add("show");
    }
    document.addEventListener("click", closeDropdownSearch);
    document.addEventListener("keydown", searchOnKeyDown);
    document.addEventListener("keyup", searchOnKeyUp);
  }
}

//We want to prepare the index only after clicking the search bar
var idx = null
const docMap = new Map()

function prepareIdxAndDocMap() {
  const docs = [  
    {
      "title": "Configuration",
      "url": "/scalambda/docs/configuration/",
      "content": "Defining Scalambdas After importing the plugin into your project, you can then use the scalambda function to define Lambda Functions from your project’s sources. Example // build.sbt // a basic lambda function using default settings lazy val example = project .enablePlugins(ScalambdaPlugin) .settings( scalambda( functionClasspath = ??? // example: \"io.carpe.example.ExampleFunction\" ) ) // you can also create multiple functions that share the same source lazy val multipleFunctions = project .enablePlugins(ScalambdaPlugin) .settings( // first function scalambda( functionClasspath = ??? // example: \"io.carpe.example.ExampleFunction\" ) // second function scalambda( functionClasspath = ??? // example: \"io.carpe.example.ExampleFunction\" ) ) The first time the scalambda function is ran within a project, sbt will automatically inject in the scalambda-core and scalambda-testing libraries. These libraries provide you with a traits and helpers to make developing and testing lambda functions quick and easy. Settings Once you have enabled the Scalambda plugin, you’ll be able to set some settings that are shared by all functions within the current project. That being said, the bulk of the configuration options are configured by modifying the parameters to each scalambda function invocation. Project Settings This is the full list of settings that are shared by all your functions. Setting Key Type Description Default Value s3BucketName String Prefix for S3 bucket name to store binaries in sbt.Keys.name billingTags Seq[BillingTag] AWS Billing Tags to apply to all terraformed resources. You can also provide billing tags via a terraform variable in the generated module. See below for details. Nil scalambdaTerraformPath File Path to where terraform should be written to sbt.Keys.target / \"terraform\" scalambdaDependenciesMergeStrat MergeStrategy sbt-assembly MergeStrategy for your dependencies jar Check it out apiName String Name for Api Gateway instance sbt.Keys.name domainName ApiDomain Domain name for Api Gateway - Lambda Settings Each scalambda function accepts a wide range of parameters. Although, the only required parameter is the functionClasspath. Parameter Type Description Default Value functionClasspath String path to the class that contains the handler for your lambda function - functionNaming FunctionNaming controls how your lambda function is named WorkspaceBased iamRoleSource FunctionRoleSource controls how your lambda function receives it’s IAM role. Options are RoleFromVariable or RoleFromArn RoleFromVariable memory Int amount of memory for your function to use (in MBs) 1536 runtime ScalambdaRuntime runtime for your function to use. Options are Java8, Java11, or GraalNative Java8 concurrencyLimit Int maximum number of concurrent instances of your Function - warmWith WarmerConfig controls how your lambda function will be kept “warm” WarmerConfig.Cold vpcConfig VpcConfig use this setting if you need to run your Lambda Function inside a VPC. Options are StaticVpcConfig and VpcFromTF VpcConf.withoutVpc environmentVariables Seq[EnvironmentVariable] use this to inject ENV variables into your Lambda Function. Options are StaticVariable and VariableFromTF Nil Note: If you use the GraalNative runtime, please make sure to read the documentation page for additional information (and a list of disclaimers a mile long). Terraform Module Settings When/if you run scalambdaTerraform, several variables will be generated in the outputted module. Parameter Type Description Default Value your_function_name_billing_tags map Billing tags for the function. These will be merged with the billing tags provided via the plugin billingTags setting. {} s3_billing_tags map Billing tags for the S3 bucket. These will be merged with the billing tags provided via the plugin billingTags setting. {} enable_xray boolean If enabled, the Api Gateway stage in this module will create traces in AWS X-Ray for each request. false access_log_cloudwatch_arn string The arn of a Cloudwatch Log Group that you’d like the Api Gateway stage to send access logs to. Make sure that you have set up a proper role for ApiGateway to use to log before using this setting. - access_log_format string The format for the Api Gateway stage’s access logs. Check the docs to see how to customize this. - Tasks The following tasks are also available to each project that has enabled the plugin. Setting Key Description scalambdaTerraform Produces a Terraform module from the project’s scalambda configuration scalambdaPackage Create jar (without dependencies) for your Lambda Function(s) scalambdaPackageDependencies Create a jar containing all the dependencies for your Lambda Function(s)"
    } ,    
    {
      "title": "Create an API",
      "url": "/scalambda/docs/api/create-api/",
      "content": "Defining your API Here is how you might define the classic Petstore API example: // build.sbt lazy val petstore = project .enablePlugins(ScalambdaPlugin) .settings({ // save the lambda function to a value so you can re-use it across multiple endpoints lazy val petsHandler = Function( functionClasspath = ??? // example: \"io.carpe.example.CreatePet\" ) // the apiGatewayDefinition function allows us name our api and map the lambda function above to http endpoints apiGatewayDefinition(apiGatewayInstanceName = \"petstore-api-${terraform.workspace}\")( // This sends all POST requests to \"&lt;my api domain&gt;/pets\" to our lambda function POST(\"/pets\") -&gt; petsHandler, // This sends all GET requests to \"&lt;my api domain&gt;/pets\" to our lambda function GET(\"/pets\") -&gt; petsHandler, // This sends all GET requests to \"&lt;my api domain&gt;/pets/&lt;some pet id&gt;\" to our lambda function // (it also makes \"id\" available as a path parameter, inside the pathParameters field on the request) GET(\"/pets/{id}\") -&gt; petsHandler ) }) As you can see in the above example, we can map the same function to multiple endpoints. This helps keep cold start times down by allowing your functions to be re-used more frequently. You can just as easily define a lambda function for each endpoint if you’d prefer. Handling Authorization There are two ways to secure your Api Gateways. The high-level steps for adding both to your API are the same: Define the desired Auth in SBT Run scalambdaTerraform to generate terraform (do not deploy anything just yet) Run terraform validate to make sure you’ve met all the requirements for your chosen Auth Auth via Api-Key Easy, but not flexible/secure enough for most use-cases Auth.ApiKey will require your users to provide an Api Key when making requests. By default, this Api Key is passed in via the X-Api-Key header. // build.sbt lazy val petstore = project .enablePlugins(ScalambdaPlugin) .settings({ val petsHandler = ??? val loginHandler = ??? // this auth config will be implicitly applied to the POST and GET methods below. // Auth.ApiKey will set the endpoint to require an Api Key via Api Gateway's Api Key service (be warned that using // only Api Key authorization is not recommended by AWS). implicit val apiGatewayApiKeyAuthorizer: Auth = Auth.ApiKey apiGatewayDefinition(apiGatewayInstanceName = \"petstore-api-${terraform.workspace}\")( POST(\"/pets\") -&gt; petsHandler, GET(\"/pets\") -&gt; petsHandler // allow all users to hit the login endpoint by explicitly passing `Auth.AllowAll` POST(\"/login\")(Auth.AllowAll) -&gt; loginHandler ) }) Auth via Lambda Authorizer Difficult, but will work for nearly any use-case If you need something more flexible than Api Keys OR you want to inject and cache data within each user’s session, you probably want to use Custom Lambda Authorizers. Before you jump in straight in though, you’ll probably want to read up on Custom Authorizers in the AWS Docs. Finished reading the documentation above? Good. So, according to AWS, there are two different authorizer types (REQUEST or TOKEN). The majority of APIs will use Authorizers of type TOKEN, but here’s how to define both of them in SBT: // build.sbt // Lambda Authorizer of type TOKEN implicit val myTokenAuthorizer: Auth = Auth.TokenAuthorizer( // this name can be anything you'd like. It will be ysed to create variables in the terraform module that is outputted by // the `scalambdaTerraform` task. tfVariableName = \"my_token_authorizer\" ) // Lambda Authorizer of type REQUEST implicit val myRequestAuthorizer: Auth = Auth.RequestAuthorizer( // this name can be anything you'd like. It will be used to create variables in the terraform module that is outputted by // the `scalambdaTerraform` task. tfVariableName = \"my_request_authorizer\", identitySources = Seq(\"method.request.header.X-Api-Key\") ) Once you’ve defined these authorizers in SBT, you’ll likely want to run scalambdaTerraform to generate terraform variables that you can use to connect your Api to the Authorizers. module \"my_api\" { // path to terraform generated by `scalambdaTerraform` (your actual path may differ from this one) source = \"./target/terraform\" // standard input variables for the pets handler lambda we created in the last example pets_handler_lambda_role_arn = \"&lt;arn:aws:iam:0123456789:role/for-the-pets-handler-function&gt;\" // These next two variables are generated if you defined either an Auth.TokenAuthorizer or Auth.RequestAuthorizer authorizer_role = \"&lt;arn:aws:iam:0123456789:role/for-the-authorizer&gt;\" // role for invoking the authorizer lambda authorizer_uri = \"arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/YourCustomAuthorizerFunction/invocations\" // invocation endpoint for the authorizer lambda function } Auth via Both Want to take advantage of AWS Usage Plans and Api Keys, but also need to inject in some custom session data? You can use Auth.Multiple to combine as many different authorization methods as you’d like. // this type of Auth config combines multiple different authorization methods. It will require both of the authorization // methods to \"pass\" before allowing the Lambda function to be invoked. implicit val requireApiKeyAndRequestAuthorizer: Auth = Auth.Multiple( // request authorizer that fetches data from the X-Api-Key header (the same one that the ApiKey looks at) Auth.RequestAuthorizer(\"authorizer\", identitySources = Seq(\"method.request.header.X-Api-Key\")), Auth.ApiKey ) Handling CORS Scalambda automatically adds OPTIONS request handling to each endpoint in your API. If you prefer to handle CORS yourself, you have a few options. Disable Scalambda’s automatic handling of CORS Here is an example of how you can disable OPTIONS request handling for the /pets route: lazy val petstore = project .enablePlugins(ScalambdaPlugin) .settings({ val petsHandler = ??? apiGatewayDefinition(apiGatewayInstanceName = \"petstore-api-${terraform.workspace}\")( // setting to CORS.AllowNone will prevent scalambda from adding a default OPTIONS request handler for \"/pets\" POST(\"/pets\", cors = CORS.AllowNone) -&gt; petsHandler, GET(\"/pets\", cors = CORS.AllowNone) -&gt; petsHandler ) }) Create a Lambda to handle OPTIONS requests Here is an example of how you use your own Lambda to handle OPTIONS requests for the /pets route: lazy val petstore = project .enablePlugins(ScalambdaPlugin) .settings({ val petsHandler = ??? val optionsHandler = ??? apiGatewayDefinition(apiGatewayInstanceName = \"petstore-api-${terraform.workspace}\")( POST(\"/pets\") -&gt; petsHandler, GET(\"/pets\") -&gt; petsHandler // this OPTIONS request handler will override the one scalambda provides by default for \"/pets\" OPTIONS(\"/pets\")(Auth.AllowAll) -&gt; optionsHandler ) }) Defining Lambdas for ApiGateway When Api Gateway receives a request, it will invoke the configured Lambda Function with what AWS calls an “Api Gateway Proxy Request”. They also expect your function to provide a response in the form of an “Api Gateway Proxy Response”. Scalambda provides both of these as traits that you can use in your Lambda Functions like so: package io.carpe.example import com.amazonaws.services.lambda.runtime.Context import io.carpe.scalambda.Scalambda import io.carpe.scalambda.request.APIGatewayProxyRequest import io.carpe.scalambda.response.{APIGatewayProxyResponse, ApiError} class Greeter extends Scalambda[APIGatewayProxyRequest[String], APIGatewayProxyResponse[String]] { /** * Accept a request that provides someone's name in a JSON body. * * Response with a greeting for that given person. * * @param input from api gateway that represents the request * @param context lambda request context * @return */ override def handleRequest(input: APIGatewayProxyRequest[String], context: Context): APIGatewayProxyResponse[String] = { val greetingResponse = for { // attempt to get the provided name from the input inputName &lt;- input.body // use it to create a greeting greeting = s\"Hello, ${inputName}!\" } yield { // place the greeting inside a response object, along with any headers that you'd like // to supply. APIGatewayProxyResponse.WithBody( statusCode = 200, headers = Map( \"content-type\" -&gt; \"application/json\" ), body = greeting ) } // return the result, or an error to Api Gateway greetingResponse.getOrElse({ APIGatewayProxyResponse.WithError( // ApiError has a default encoder that will be used to inject errors into the // response body as json. You can override this encoder if you'd like, it is an implicit // parameter for the APIGatewayProxyResponse.WithError's constructor error = ApiError.InputError(\"No input was provided\"), headers = Map( \"content-type\" -&gt; \"application/json\" ) ) }) } } As you can see, there really isn’t too much of a difference between a Lambda Function that serves requests from Api Gateway and one that does not. The only thing that changes is the input to your Function."
    } ,    
    {
      "title": "Deploy an API",
      "url": "/scalambda/docs/api/deploy-api/",
      "content": "This section assumes you have already defined your API, read this to learn how. Deploying your API Since all the configuration for your API is inside your build.sbt file, follow the same steps for deploying a Scalambda project that uses Api Gateway as you would with one that doesn’t. Step 1: Generate the Terraform module Use the scalambdaTerraform SBT task to generate a Terraform module for your API. Step 2: Add the new Terraform module Either in your existing Terraform, or in a new .tf file, add the following module: # my-project/terraform/main.tf module \"helloworld_api\" { # Points to the terraform generated by scalambda inside the your project's target folder source = \"&lt;path to your project&gt;/target/terraform\" } Step 3: Apply the Terraform Run the following commands in your favorite shell (make sure you have Terraform installed): # cd to wherever your terraform is cd my-project/terraform # initialize the terraform (only need to this once) terraform init # apply the terraform terraform apply"
    } ,    
    {
      "title": "Deploying Functions",
      "url": "/scalambda/docs/deploying-functions/",
      "content": "Deploying Functions Assuming you already have an AWS account and have installed sbt and Terraform, you can have a function deployed in less than 5 minutes (for real, we did it live during a few internal presentations to prove it). Run the scalambdaTerraform SBT task. This will generate a Terraform module (by default it will be placed inside your project’s target/ folder). You can then use this module to deploy your project. Super easy, right? You can do pretty much anything you want with the Terraform that Scalambda generates. You could package it and hand it off to your dev-ops team, throw it in a Docker image, whatever you’d like! At a high-level, the deploy process for almost all Scalambda application is: generate a Terraform module, then apply the Terraform module. An Opinionated Deployment Process In this section, we’re going to show you what we (at Carpe Data) do with the Terraform, just so you can get an idea of how it might be used before you brainstorm for yourself how it best fits your workflow. First, some prerequisites. Make sure you’ve done the following: Install sbt Install Terraform Install AWS CLI (Optional) Step 1) Configure Terraform Let’s create a main.tf file that references the generated terraform, using it as a module. This is what we’ll use to set up the provider (and maybe a backend?), which will allow us to communicate with the AWS API, and track our infrastructure’s state. # This block of terraform is a backend. It's totally optional, but highly recommended! # terraform { backend \"s3\" { # the region you'd like to store the state files in (not necessarily where your infrastructure will be provisioned) region = \"us-west-2\" # replace the value with the name of an S3 Bucket bucket = \"&lt;name of S3 bucket you'd like to place the state in&gt;\" # replace these two values with your project's name workspace_key_prefix = \"&lt;your-project-name&gt;\" key = \"&lt;your-project-name&gt;.tfstate\" encrypt = true } } # This block is the provider. It will be used by Scalambda to provision your infrastructure (i.e. the Lambda Functions) # for you. # # Disclaimer: It will pull AWS credentials from whatever machine you run this terraform, so if you've setup the AWS CLI, # you don't have to worry about providing it credentials or anything. # provider \"aws\" { # This will be the region that your infrastructure is provisioned in. Replace it with your desired region if you want! region = \"us-west-2\" } # This will be what connects the two resources above to the Terraform you generated in step 1. # # Note: You can name this module whatever you would like! Just replace \"my_lambda_functions\" with whatever you'd like. # module \"my_lambda_functions\" { # This should be the relative path to the terraform generated by Scalambda. # # In this example, this main.tf file will just be in the root of our project, so it will be in the same directory as # our project's `src` and `target` folders. Which makes this source path really easy. source = \"./target/terraform\" # Based on how you configured your Lambda Functions in your build.sbt file, you may or may not need to provide some # inputs to this module. You can do that now if you already know what you need, or you can wait for Terraform to show # you what it needs via some nice error messages during the init/apply step (which is what's next on this guide). } If you’re at all confused: You can generate a project with our Giter8 Template, or just check out at the files here to get an idea of what the layout looks like. Step 2) Apply the Terraform Now all that’s left to do is initialize and apply the Terraform. From the same directory as your main.tf file, run the following: # Initialize the Terraform terraform init # Apply the Terraform terraform apply It’s likely that after running terraform init or terraform apply, you will see some errors, don’t worry! We’re super close to being done anyway, and the errors should be fairly easy to fix. Check out the section below if you need help, or consider opening an issue on our Github repository. Step 3) Done! After you’ve fixed the errors and your Terraform has been applied, you should be able to see your newly provisioned Lambda Functions in the AWS Web Console. For subsequent deploys, all you need to do is run the following: # Use Scalambda to generate the Terraform, which will # recompile your code to get the latest changes sbt scalambdaTerraform # Apply the newly generated Terraform. terraform apply Since Terraform manages your infrastructure’s state and performs incremental changes, this will be MUCH faster than the first deploy! Which means you can quickly and iteratively make changes to your code and test them out on AWS Lambda itself if you’d like. Common Problems/Solutions Problem: Terraform is complaining about missing variables for the module we created Solution: Fill those missing variables in! You can find some sensible defaults for stuff like your function’s role over on the Giter8 Template. Problem: Terraform is complaining about missing AWS credentials Solution: If you have the aws cli installed, try running something simple like aws s3 ls to make sure your credentials are valid. If not, go ahead and install the aws cli as that will allow you to run aws configure. From our experience, it’s both the fastest, and most effective way to solve this problem. Alternatively, you could also check out the documentation on managing credentials for the AWS Terraform Provider."
    } ,    
    {
      "title": "Getting Started",
      "url": "/scalambda/docs/",
      "content": "Scalambda Deploying Lambda functions is time-consuming, so we built Scalambda to make it quick and easy. Using Scalambda, you can enable developers to easily build and deploy their own Lambda Functions (and/or ApiGateway instances) with little to no effort or knowledge of AWS required. Create a new Project The easiest way to get started with Scalambda is via the Giter8 template. Run the following to get started immediately: sbt --supershell=false new carpe/scalambda.g8 You can check out the repository as well if you’d like here. Add to an existing Project Add the plugin to your project in the project/plugins.sbt file: addSbtPlugin(\"io.carpe\" % \"sbt-scalambda\" % \"6.4.0\") After you add the plugin, you’ll probably wanna read through the documentation for how to configure your Lambda Functions. Motivation Our motivations for Scalambda were: Make it so anybody can deploy an API to AWS with two or less steps Simplify configuration and optimization of Lambda Functions Speed up the time to market of new projects by providing as many sensible defaults as we could Give developers as much freedom as possible so they can be free to experiment with new ideas Scalambda started as an internal-only project over a year ago. Over the course of its lifetime it has received a TON of feedback and refinement from several of our teams and friends. Thanks to their efforts, we think we’ve managed to land on a solution that is an incredibly powerful tool. What is it? Scalambda itself is composed of three separate libraries. Each of them can be used independently depending on your project’s use case and your team’s toolchain. sbt-scalambda An SBT plugin that should help you to deploy your lambdas, managing libraries, logging and much more. scalambda-core A traditional library that provides utilities for writing Scala-based Lambda Functions scalambda-testing A set of test helpers for testing Lambda Functions (Short-term) Roadmap Top priority is continue to create more documentation as well as add some example projects to help people get their Lambda Functions deployed even quicker. In the meantime, if you have any questions, please don’t hesitate to Open an Issue on our Github repo!"
    } ,    
    {
      "title": "Using Graal Native",
      "url": "/scalambda/docs/thegraaaaal/",
      "content": "What and why? If you’re not yet familiar with GraalVM’s Native Image builder, it essentially allows you to turn your Java code into a native executable. This essentially allows you to run your code without a JVM, which allows for nearly instantaneous start times. It also allows for some pretty awesome optimizations like the ability to initialize certain objects at compile time. Both of these advantages by themselves are incredibly powerful tools that can allow you to decimate your function’s cold start times. Disclaimers GraalVM’s Native Image builder isn’t quite magic, and comes with some serious risks. The good news is, the problems with Graal Native tend to come up at compile time, rather than during your function’s runtime. However, debugging/fixing these build issues with your native images can be incredibly costly. One of my favorite posts that I think perfectly demonstrates just how time-consuming and intensive debugging these issues can be is this post, which talks about getting Netty to work with Graal Native. Imagine the amount of time that went into figuring out the issues that post talks about. Spoiler alert: it takes a ton of time. If your function is incredibly simple with few dependencies, then you might be able to get away with turning your function into a Native Image. Otherwise, try to look at other ways of reducing your cold start times. TL;DR: Don’t use Graal Native if you aren’t prepared to invest huge amounts of time debugging a ton of build issues. If you have a dedicated team at your company that solves these kinds of issues, be prepared for everyone on that team to start hating you and don’t expect them to be able to get your function to actually work. All that being said, you can always revert to the Java11 or Java8 runtime if you can’t get your function to work in GraalNative mode. So if you’ve some time you want to kill, you can follow the guide below to give Graal Native a shot to see if it works with your function. Configuration One quick note before you start, I highly recommend that you try deploying a Java8 or Java11 with Scalambda first, before jumping into the deep end. You can do it in under 5 minutes using our g8 template and it will give you a perfect plan B if/when you run into issues with Graal Native. Prerequisites Scalambda uses SBT Native Packager under the hood. Before running scalambdaTerraform, you’ll need to make sure you have installed all of the requirements. build.sbt Below is an example build.sbt file for a fully configured lambda function using a Native Image. lazy val nativegreeter = (project in file(\".\")) .enablePlugins(ScalambdaPlugin) .settings( // this call enables Scalambda, and sets the class found at this path to be the handler scalambda(\"science.doing.nativegreeter.NativeGreeter\", runtime = GraalNative, memory = 256) ).settings( // graal native image settings // Options used by `native-image` when building native image // https://www.graalvm.org/docs/reference-manual/native-image/ graalVMNativeImageOptions ++= Seq( \"--initialize-at-build-time\", // Auto-packs dependent libs at build-time \"--no-fallback\", // Bakes-in run-time reflection (alternately: --auto-fallback, --force-fallback) \"--no-server\", // Won't be running `graalvm-native-image:packageBin` often, so one less thing to break \"--static\", // Forces statically-linked binary, requires libc installation. Comment this out if you're using OSX \"--enable-url-protocols=http\" // Enables http requests, which are required in order to communicate with the AWS Lambda Runtime API // \"--enable-url-protocols=http,https\" // Enables both http and https requests ) ) You will almost certainly need to tweak the configuration a bit depending on the needs for your function. You will almost certainly need to tweak the settings above in order for your code to successfully build. Checkout the full list of available settings in sbt-native-packager’s documentation. Important Note: Due to current limitations on how we assemble your native image, each sub-project can only include one GraalNative Scalambda Function. Implementation In order to get your function to execute properly, you only need to do two things. Your main class must be an object. Your main class must extend either io.carpe.scalambda.native.ScalambdaIO or io.carpe.scalambda.native.Scalambda. Scalambda automatically injects the library that includes the io.carpe.scalambda.native package when you set your function’s runtime to GraalNative. Other than these two things, you shouldn’t need to change anything else at all from the usual code you’d use for a JVM-based Scalambda Function. Of course, you may want to tweak things later. package science.doing.nativegreeter import cats.effect.IO import io.carpe.scalambda.native.ScalambdaIO // NOTE: we are using the `native` ScalambdaIO, not the JVM based one. object NativeGreeter extends ScalambdaIO[String, String] { override def run(input: String): IO[String] = IO { \"Hello, \" + input + \"!\" } } Building and Deploying Assuming you already have the prerequisites installed, you can try to deploy your new Graal Native Lambda function the same way as any other Scalambda Lambda Function. Just run sbt scalambdaTerraform to generate the terraform and then apply it. For further details, checkout Deploying Functions for a more in-depth explanation."
    } ,          
    {
      "title": "Writing Functions",
      "url": "/scalambda/docs/writing-functions/",
      "content": "Writing Lambdas with Scalambda Scalambda automatically adds scalambda-core as a dependency to your project when you enable it. It is a library designed to help you get started coding lambda functions as quickly as possible. The scalambda-core library currently comes with two traits for defining Lambda functions. Both use circe for encoding and decoding of your function’s input and output. io.carpe.scalambda.Scalambda gives you the most freedom and control for your functions. io.carpe.scalambda.effect.ScalambdaIO allows you to write functions using cats-effect’s powerful IO. package io.carpe import com.amazonaws.services.lambda.runtime.Context import io.carpe.scalambda.Scalambda class HelloWorld extends Scalambda[String, String] { override def handleRequest(input: String, context: Context): String = { \"Hello, ${input}!\" } } Inputs and Outputs You can use any type you want for the input and output of your function, so long as you’ve defined a Encoder and Decoder for that given type. You have a few options for how you’d like to define these encoders and decoders. At Carpe Data, we like to use Semi-Automatic Derivation. By placing these encoders and decoders into companion objects within your Input and Output classes, you can guarantee that they will be in scope for your functions. Example Here is a custom case class we want to use as the output and/or input to our Lambda Function. package io.carpe.views case class Car(make: String, model: String) object Car { import io.circe.generic.extras.semiauto._ import io.circe.{Decoder, Encoder} implicit val decoder: Decoder[Car] = deriveConfiguredDecoder[Car] implicit val encoder: Encoder[Car] = deriveConfiguredEncoder[Car] } This function accepts a Car as an argument, then automatically turns that Car into a nice sports car. A dumb example, but hopefully it gives you the general idea. Since it imports Car, the implicit decoder and encoder for a Car will be made visible to Scalambda. This allows Scalambda to use it to decode the input and encode the output for you, package io.carpe import com.amazonaws.services.lambda.runtime.Context import io.carpe.scalambda.Scalambda import io.carpe.views.Car class UpgradeCar extends Scalambda[Car, Car] { override def handleRequest(input: Car, context: Context): Car = { // turn the input into a 911, then return it input.copy(make = \"Porsche\", model = \"911 GT3\") } }"
    }    
  ];

  idx = lunr(function () {
    this.ref("title");
    this.field("content");

    docs.forEach(function (doc) {
      this.add(doc);
    }, this);
  });

  docs.forEach(function (doc) {
    docMap.set(doc.title, doc.url);
  });
}

// The onkeypress handler for search functionality
function searchOnKeyDown(e) {
  const keyCode = e.keyCode;
  const parent = e.target.parentElement;
  const isSearchBar = e.target.id === "search-bar";
  const isSearchResult = parent ? parent.id.startsWith("result-") : false;
  const isSearchBarOrResult = isSearchBar || isSearchResult;

  if (keyCode === 40 && isSearchBarOrResult) {
    // On 'down', try to navigate down the search results
    e.preventDefault();
    e.stopPropagation();
    selectDown(e);
  } else if (keyCode === 38 && isSearchBarOrResult) {
    // On 'up', try to navigate up the search results
    e.preventDefault();
    e.stopPropagation();
    selectUp(e);
  } else if (keyCode === 27 && isSearchBarOrResult) {
    // On 'ESC', close the search dropdown
    e.preventDefault();
    e.stopPropagation();
    closeDropdownSearch(e);
  }
}

// Search is only done on key-up so that the search terms are properly propagated
function searchOnKeyUp(e) {
  // Filter out up, down, esc keys
  const keyCode = e.keyCode;
  const cannotBe = [40, 38, 27];
  const isSearchBar = e.target.id === "search-bar";
  const keyIsNotWrong = !cannotBe.includes(keyCode);
  if (isSearchBar && keyIsNotWrong) {
    // Try to run a search
    runSearch(e);
  }
}

// Move the cursor up the search list
function selectUp(e) {
  if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index) && (index > 0)) {
      const nextIndexStr = "result-" + (index - 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Move the cursor down the search list
function selectDown(e) {
  if (e.target.id === "search-bar") {
    const firstResult = document.querySelector("li[id$='result-0']");
    if (firstResult) {
      firstResult.firstChild.focus();
    }
  } else if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index)) {
      const nextIndexStr = "result-" + (index + 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Search for whatever the user has typed so far
function runSearch(e) {
  if (e.target.value === "") {
    // On empty string, remove all search results
    // Otherwise this may show all results as everything is a "match"
    applySearchResults([]);
  } else {
    const tokens = e.target.value.split(" ");
    const moddedTokens = tokens.map(function (token) {
      // "*" + token + "*"
      return token;
    })
    const searchTerm = moddedTokens.join(" ");
    const searchResults = idx.search(searchTerm);
    const mapResults = searchResults.map(function (result) {
      const resultUrl = docMap.get(result.ref);
      return { name: result.ref, url: resultUrl };
    })

    applySearchResults(mapResults);
  }

}

// After a search, modify the search dropdown to contain the search results
function applySearchResults(results) {
  const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
  if (dropdown) {
    //Remove each child
    while (dropdown.firstChild) {
      dropdown.removeChild(dropdown.firstChild);
    }

    //Add each result as an element in the list
    results.forEach(function (result, i) {
      const elem = document.createElement("li");
      elem.setAttribute("class", "dropdown-item");
      elem.setAttribute("id", "result-" + i);

      const elemLink = document.createElement("a");
      elemLink.setAttribute("title", result.name);
      elemLink.setAttribute("href", result.url);
      elemLink.setAttribute("class", "dropdown-item-link");

      const elemLinkText = document.createElement("span");
      elemLinkText.setAttribute("class", "dropdown-item-link-text");
      elemLinkText.innerHTML = result.name;

      elemLink.appendChild(elemLinkText);
      elem.appendChild(elemLink);
      dropdown.appendChild(elem);
    });
  }
}

// Close the dropdown if the user clicks (only) outside of it
function closeDropdownSearch(e) {
  // Check if where we're clicking is the search dropdown
  if (e.target.id !== "search-bar") {
    const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
    if (dropdown) {
      dropdown.classList.remove("show");
      document.documentElement.removeEventListener("click", closeDropdownSearch);
    }
  }
}
