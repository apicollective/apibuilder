    implicit val jsonReadsApiDocCode_Target = __.read[String].map(Code.Target.apply)
    
    implicit val jsonWritesApiDocCode_Target = new Writes[Code.Target] {
      def writes(x: Code.Target) = JsString(x.toString)
    }
