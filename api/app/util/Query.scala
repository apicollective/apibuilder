package util

import anorm._
import java.util.UUID
import org.joda.time.DateTime

sealed trait BindVariable {

  def name: String
  def functions: Seq[Query.Function]
  def value: Any
  def sql: String
  def toNamedParameter(): NamedParameter

}

object BindVariable {

  case class Int(override val name: String, override val value: Number) extends BindVariable {
    override val sql = s"{$name}::int"
    override val functions = Nil
    override def toNamedParameter() = NamedParameter(name, value.toString)
  }

  case class BigInt(override val name: String, override val value: Number) extends BindVariable {
    override val sql = s"{$name}::bigint"
    override val functions = Nil
    override def toNamedParameter() = NamedParameter(name, value.toString)
  }

  case class Num(override val name: String, override val value: Number) extends BindVariable {
    override val sql = s"{$name}::numeric"
    override val functions = Nil
    override def toNamedParameter() = NamedParameter(name, value.toString)
  }

  case class Str(override val name: String, override val value: String) extends BindVariable {
    override val sql = s"{$name}"
    override val functions = Seq(Query.Function.Trim)
    override def toNamedParameter() = NamedParameter(name, value)
  }

  case class Uuid(override val name: String, override val value: UUID) extends BindVariable {
    override val sql = s"{$name}::uuid"
    override val functions = Nil
    override def toNamedParameter() = NamedParameter(name, value.toString)
  }

  case class DateTimeVar(override val name: String, override val value: DateTime) extends BindVariable {
    override val sql = s"{$name}::timestamptz"
    override val functions = Nil
    override def toNamedParameter() = NamedParameter(name, value.toString)
  }
  
  case class Unit(override val name: String) extends BindVariable {
    override val value = None
    override val sql = s"{$name}"
    override val functions = Nil
    override def toNamedParameter() = NamedParameter(name, Option.empty[String])
  }

  private[this] val LeadingUnderscores = """^_+""".r
  private[this] val MultiUnderscores = """__+""".r
  private[this] val TrailingUnderscores = """_+$""".r
  private[this] val ScrubName = """[^\w\d\_]""".r

  def safeName(name: String): String = {
    val idx = name.lastIndexOf(".")
    val simpleName = if (idx > 0) { name.substring(idx + 1) } else { name }.toLowerCase.trim

    val safeName = LeadingUnderscores.replaceAllIn(
      TrailingUnderscores.replaceAllIn(
        MultiUnderscores.replaceAllIn(
          ScrubName.replaceAllIn(simpleName.trim, "_"),
          "_"
        ),
        ""),
      ""
    )

    safeName match {
      case "" => "bind"
      case _ => safeName
    }
  }

}

object Query {

  trait Function

  /**
   * Helper trait to allow caller to apply functions to the columns or
   * values in the query. A common use case is to apply
   * lower(text(...)) functions to enable case insensitive text
   * matching.
   */
  object Function {

    case object Lower extends Function {
      override def toString = "lower"
    }

    case object Trim extends Function {
      override def toString = "trim"
    }

    case class Custom(name: String) extends Function {
      override def toString = name
    }

  }

}

case class Query(
  base: String,
  conditions: Seq[String] = Nil,
  bind: Seq[BindVariable] = Nil,
  orderBy: Seq[String] = Nil,
  limit: Option[Long] = None,
  offset: Option[Long] = None,
  debug: Boolean = false,
  groupBy: Seq[String] = Nil
) {

  def equals[T](column: String, value: Option[T]): Query = optionalOperation(column, "=", value)
  def equals[T](column: String, value: T): Query = operation(column, "=", value)

  def notEquals[T](column: String, value: Option[T]): Query = optionalOperation(column, "!=", value)
  def notEquals[T](column: String, value: T): Query = operation(column, "!=", value)

  def lessThan[T](column: String, value: Option[T]): Query = optionalOperation(column, "<", value)
  def lessThan[T](column: String, value: T): Query = operation(column, "<", value)

  def lessThanOrEquals[T](column: String, value: Option[T]): Query = optionalOperation(column, "<=", value)
  def lessThanOrEquals[T](column: String, value: T): Query = operation(column, "<=", value)

  def greaterThan[T](column: String, value: Option[T]): Query = optionalOperation(column, ">", value)
  def greaterThan[T](column: String, value: T): Query = operation(column, ">", value)

  def greaterThanOrEquals[T](column: String, value: Option[T]): Query = optionalOperation(column, ">=", value)
  def greaterThanOrEquals[T](column: String, value: T): Query = operation(column, ">=", value)

  def operation[T](
    column: String,
    operator: String,
    value: T,
    columnFunctions: Seq[Query.Function] = Nil,
    valueFunctions: Seq[Query.Function] = Nil
  ): Query = {
    val bindVar = toBindVariable(uniqueBindName(column), value)
    val exprColumn = withFunctions(column, columnFunctions)
    val exprValue = withFunctions(bindVar.sql, valueFunctions ++ bindVar.functions)

    this.copy(
      conditions = conditions ++ Seq(s"$exprColumn $operator $exprValue"),
      bind = bind ++ Seq(bindVar)
    )
  }

  def optionalOperation[T](
    column: String,
    operator: String,
    value: Option[T],
    columnFunctions: Seq[Query.Function] = Nil,
    valueFunctions: Seq[Query.Function] = Nil
  ): Query = {
    value match {
      case None => this
      case Some(v) => operation(column, operator, v, columnFunctions, valueFunctions)
    }
  }

  def optionalIn[T](
    column: String,
    values: Option[Seq[T]],
    columnFunctions: Seq[Query.Function] = Nil,
    valueFunctions: Seq[Query.Function] = Nil
  ): Query = {
    values match {
      case None => this
      case Some(v) => in(column, v, columnFunctions, valueFunctions)
    }
  }

  def in[T](
    column: String,
    values: Seq[T],
    columnFunctions: Seq[Query.Function] = Nil,
    valueFunctions: Seq[Query.Function] = Nil
  ): Query = {
    values match {
      case Nil => {
        this.copy(
          conditions = conditions ++ Seq("false")
        )
      }
      case multiple => {
        val bindVariables = multiple.zipWithIndex.map { case (value, i) =>
          val n = if (i == 0) { column } else { s"${column}${i+1}" }
          toBindVariable(uniqueBindName(n), value)
        }

        val exprColumn = withFunctions(column, columnFunctions)

        val cond = s"$exprColumn in (%s)".format(
          bindVariables.map { bindVar =>
            withFunctions(bindVar.sql, valueFunctions ++ bindVar.functions)
          }.mkString(", ")
        )
        this.copy(
          conditions = conditions ++ Seq(cond),
          bind = bind ++ bindVariables
        )
      }
    }
  }

  def or(
    clauses: Seq[String]
  ): Query = {
    clauses match {
      case Nil => this
      case one :: Nil => and(one)
      case multiple => and("(" + multiple.mkString(" or ") + ")")
    }
  }

  def or(
    clause: Option[String]
  ): Query = {
    clause match {
      case None => this
      case Some(v) => or(v)
    }
  }

  def or(
    clause: String
  ): Query = {
    and(clause)
  }

  def and(
    clauses: Seq[String]
  ): Query = {
    this.copy(conditions = conditions ++ clauses)
  }

  def and(
    clause: String
  ): Query = {
    and(Seq(clause))
  }

  def and(
    clause: Option[String]
  ): Query = {
    clause match {
      case None => this
      case Some(v) => and(v)
    }
  }

  /**
    * Adds a bind variable to this query. You will receive a runtime
    * error if this bind variable is already defined.
    */
  def bind[T](
    name: String,
    value: T
  ): Query = {
    bind.find(_.name == name) match {
      case None => {
        this.copy(bind = bind ++ Seq(toBindVariable(name, value)))
      }
      case Some(_) => {
        sys.error(s"Bind variable named '$name' already defined")
      }
    }
  }

  def bind[T](
    name: String,
    value: Option[T]
  ): Query = {
    value match {
      case None => bind(name, ())
      case Some(v) => bind(name, v)
    }
  }

  def optionalText[T](
    column: String,
    value: Option[T],
    columnFunctions: Seq[Query.Function] = Nil,
    valueFunctions: Seq[Query.Function] = Seq(Query.Function.Trim)
  ): Query = {
    optionalOperation(
      column = column,
      operator = "=",
      value = value,
      columnFunctions = columnFunctions,
      valueFunctions = valueFunctions
    )
  }

  def text[T](
    column: String,
    value: T,
    columnFunctions: Seq[Query.Function] = Nil,
    valueFunctions: Seq[Query.Function] = Seq(Query.Function.Trim)
  ): Query = {
    operation(
      column = column,
      operator = "=",
      value = value,
      columnFunctions = columnFunctions,
      valueFunctions = valueFunctions
    )
  }

  def boolean(column: String, value: Option[Boolean]): Query = {
    value match {
      case None => this
      case Some(v) => boolean(column, v)
    }
  }

  def boolean(column: String, value: Boolean): Query = {
    and(
      value match {
        case true => s"$column is true"
        case false => s"$column is false"
      }
    )
  }

  def isNull(column: String): Query = {
    and(s"$column is null")
  }

  def isNotNull(column: String): Query = {
    and(s"$column is not null")
  }

  def nullBoolean(column: String, value: Option[Boolean]): Query = {
    value match {
      case None => this
      case Some(v) => nullBoolean(column, v)
    }
  }

  def nullBoolean(column: String, value: Boolean): Query = {
    and(
      value match {
        case true => s"$column is not null"
        case false => s"$column is null"
      }
    )
  }

  def groupBy(value: Option[String]): Query ={
    value match {
      case None => this
      case Some(v) => groupBy(v)
    }
  }

  def groupBy(value: String): Query = {
    this.copy(groupBy = groupBy ++ Seq(value))
  }

  def orderBy(value: Option[String]): Query = {
    value match {
      case None => this
      case Some(v) => orderBy(v)
    }
  }

  def orderBy(value: String): Query = {
    this.copy(orderBy = orderBy ++ Seq(value))
  }

  def optionalLimit(value: Option[Long]): Query = {
    value match {
      case None => this
      case Some(v) => limit(v)
    }
  }

  def limit(value: Long): Query = {
    this.copy(limit = Some(value))
  }

  def optionalOffset(value: Option[Long]): Query = {
    value match {
      case None => this
      case Some(v) => offset(v)
    }
  }

  def offset(value: Long): Query = {
    this.copy(offset = Some(value))
  }

  /**
    * Creates the full text of the sql query
    */
  def sql(): String = {
    val query = conditions match {
      case Nil => base
      case conds => {
        base + " where " + conds.mkString(" and ")
      }
    }

    Seq(
      Some(query),
      groupBy match {
        case Nil => None
        case clauses => Some("group by " + clauses.mkString(", "))
      },
      orderBy match {
        case Nil => None
        case clauses => Some("order by " + clauses.mkString(", "))
      },
      limit.map(v => s"limit $v"),
      offset.map(v => s"offset $v")
    ).flatten.mkString(" ")
  }

  /**
   * Turns on debugging of this query.
   */
  def withDebugging(): Query = {
    this.copy(
      debug = true
    )
  }

  /**
   * Useful only for debugging. Returns the sql query with all bind
   * variables interpolated for easy inspection.
   */
  def interpolate(): String = {
    bind.foldLeft(sql()) { case (query, bindVar) =>
      bindVar match {
        case BindVariable.Int(name, value) => {
          query.
            replace(bindVar.sql, value.toString).
            replace(s"{$name}", value.toString)
        }
        case BindVariable.BigInt(name, value) => {
          query.
            replace(bindVar.sql, value.toString).
            replace(s"{$name}", value.toString)
        }
        case BindVariable.Num(name, value) => {
          query.
            replace(bindVar.sql, value.toString).
            replace(s"{$name}", value.toString)
        }
        case BindVariable.Uuid(_, value) => {
          query.replace(bindVar.sql, s"'$value'::uuid")
        }
        case BindVariable.DateTimeVar(_, value) => {
          query.replace(bindVar.sql, s"'$value'::timestamptz")
        }
        case BindVariable.Str(_, value) => {
          query.replace(bindVar.sql, s"'$value'")
        }
        case BindVariable.Unit(_) => {
          query.replace(bindVar.sql, "null")
        }
      }
    }
  }

  def as[T](
    parser: anorm.ResultSetParser[T]
  ) (
    implicit c: java.sql.Connection
  ) = {
    anormSql().as(parser)
  }

  /**
    * Returns debugging information about this query
    */
  def debuggingInfo(): String = {
    if (bind.isEmpty) {
      interpolate
    } else {
      Seq(
        sql,
        bind.map { bv => s" - ${bv.name}: ${bv.value}" }.mkString("\n"),
        "Interpolated:",
        interpolate
      ).mkString("\n")
    }
  }

  /**
    * Prepares the sql query for anorm, including any bind variables.
    */
  def anormSql(): anorm.SimpleSql[anorm.Row] = {
    if (debug) {
      println(debuggingInfo())
    }
    SQL(sql).on(bind.map(_.toNamedParameter): _*)
  }


  /**
    * Generates a unique, as friendly as possible, bind variable name
    */
  private[this] def toBindVariable(name: String, value: Any): BindVariable = {
    value match {
      case v: UUID => BindVariable.Uuid(name, v)
      case v: DateTime => BindVariable.DateTimeVar(name, v)
      case v: Int => BindVariable.Int(name, v)
      case v: Long => BindVariable.BigInt(name, v)
      case v: Number => BindVariable.Num(name, v)
      case v: String => BindVariable.Str(name, v)
      case v: Unit => BindVariable.Unit(name)
      case _ => BindVariable.Str(name, value.toString)
    }
  }

  /**
    * Generates a unique bind variable name from the specified input
    *
    * @param name Preferred name of bind variable - will be used if unique,
    *             otherwise we generate a unique version.
    */
  def uniqueBindName(name: String): String = {
    val safeName = BindVariable.safeName(name)

    bind.find(_.name == safeName) match {
      case Some(_) => uniqueBindName(s"${safeName}${bind.size + 1}")
      case None => safeName
    }
  }

  private[this] def withFunctions(name: String, options: Seq[Query.Function]): String = {
    options.distinct.reverse.foldLeft(name) { case (value, option) =>
      if (option.toString == "array")
        s"$option[$value]"
      else
        s"$option($value)"
    }
  }

}
