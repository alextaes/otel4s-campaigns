package promotions
package service

import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.{Async, Sync}
import doobie._
import doobie.implicits._
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.Tracer
import promotions.PostgreConfiguration
import promotions.model.Promotion

class PromotionsPostgresRepository[F[_] : Sync : Async : Tracer](configuration: PostgreConfiguration) {

  private val xa = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    configuration.url,
    "promotions_user",
    "promotions_password"
  )


  def getByIds(ids: NonEmptyList[Long]): F[List[Promotion]] = {
    Tracer[F]
      .span("postgres.getByIds", Attribute("ids", ids.toList.mkString(",")))
      .surround {
        (fr"select * from promotions where" ++ Fragments.in(fr"id", ids))
          .query[Promotion]
          .stream
          .transact(xa)
          .compile
          .toList
      }
  }

  def getById(id: Long): F[Option[Promotion]] = {
    Tracer[F]
      .span("postgres.getByIds", Attribute("id", id.toString))
      .surround {
        sql"select * from promotions where id = $id"
          .query[Promotion]
          .stream
          .transact(xa)
          .compile
          .toList
          .map(_.headOption)
      }
  }

  def create(promotion: Promotion): F[Promotion] = {
    Tracer[F]
      .span("postgres.create")
      .surround {
        for {
          id <-
            sql"""insert into promotions (campaign, title, description, created_at, created_by, modified_at, modified_by) values
              (${promotion.campaign}, ${promotion.title}, ${promotion.description}, ${promotion.createdAt}, ${promotion.createdBy}, ${promotion.modifiedAt}, ${promotion.modifiedBy})"""
            .update
            .withUniqueGeneratedKeys[Long]("id")
            .transact(xa)

          createdpromotion <- sql"select * from promotions where id = $id"
            .query[Promotion]
            .unique
            .transact(xa)
        } yield createdpromotion
      }
  }

  def update(promotion: Promotion): F[Int] = {
    Tracer[F]
      .span("postgres.update", Attribute("id", promotion.id.toString))
      .surround {
        sql"""update promotions set campaign = ${promotion.campaign},
          title = ${promotion.title},
          description = ${promotion.description},
          created_at = ${promotion.createdAt},
          created_by = ${promotion.createdBy},
          modified_at = ${promotion.modifiedAt},
          modified_by = ${promotion.modifiedBy}
          where id = ${promotion.id}"""
          .update
          .run
          .transact(xa)
      }
  }

  def delete(id: Long): F[Int] = {
    Tracer[F]
      .span("postgres.delete", Attribute("id", id.toString))
      .surround {
        sql"delete from promotions where id = $id"
          .update
          .run
          .transact(xa)
      }
  }
}
