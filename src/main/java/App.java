import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;
import static spark.Spark.*;
import java.util.List;

public class App {
  public static void main(String[] args) {
    staticFileLocation("/public");
    String layout = "templates/layout.vtl";

    get("/", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("template", "templates/index.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    //ORDERS

    get("/servers/orders/active", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("orders", Order.getAllActive());
      model.put("dishes", Dish.all());
      model.put("template", "templates/orders-active.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    //Order - take a new order
    post("/orders/new", (request, response) -> {
      int table = Integer.parseInt(request.queryParams("table"));
      int seat = Integer.parseInt(request.queryParams("seat"));
      for (Dish dish : Dish.all()) {
        if (dish.hasEnoughIngredients() && dish.getNumberPossibleDishes() > 0) {
          Integer dishQuantity = Integer.parseInt(request.queryParams(dish.getName()));
          if (dishQuantity > 0) {
            for (Integer i = dishQuantity; i > 0; i--) {
              Order order = new Order (table, seat, dish.getId());
              if (Dish.find(order.getDishId()).hasEnoughIngredients()) {
                order.save();
                order.make();
              }
              if (request.queryParams(dish.getId() + "comments") != null) {
                order.addComments(request.queryParams(dish.getId() + "comments"));
              }
            }
          }
        }
      }
      response.redirect("/servers/orders/active");
      return null;
    });

    //Order - pay for an order
    post("/servers/orders/:id/pay", (request, response) -> {
      Order thisOrder = Order.find(Integer.parseInt(request.params("id")));
      thisOrder.pay();
      response.redirect("/servers/orders/" + Integer.parseInt(request.params("id")));
      return null;
    });

    //Order - complete an order and make it no longer active
    post("/servers/orders/:id/complete", (request, response) -> {
      Order thisOrder = Order.find(Integer.parseInt(request.params("id")));
      thisOrder.complete();
      response.redirect("/servers/orders/active");
      return null;
    });

    //Order - cancel and lost ingredients i.e diner walked out
    post("/servers/orders/active/remove", (request, response) -> {
      Order thisOrder = Order.find(
        Integer.parseInt(request.queryParams("order-remove")));
      thisOrder.complete();
      response.redirect("/servers/orders/active");
      return null;
    });

    //Order - chef routing
    get("/kitchen/orders/active", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("orders", Order.getAllActiveKitchenSort());
      model.put("dishes", Dish.all());
      model.put("template", "templates/orders-kitchen.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    get("kitchen/orders/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("order", Order.find(Integer.parseInt(request.params("id"))));
      model.put("dishes", Dish.all());
      model.put("template", "templates/order-kitchen.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    post("/kitchen/orders/:id/up", (request, response) -> {
      Order thisOrder = Order.find(Integer.parseInt(request.params("id")));
      thisOrder.setIsUp();
      response.redirect("/kitchen/orders/" + Integer.parseInt(request.params("id")));
      return null;
    });

    //Order - server routing
    get("/servers/orders/new", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("orders", Order.getAllActive());
      model.put("dishes", Dish.all());
      model.put("dishclass", Dish.class);
      model.put("template", "templates/orders-new.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    get("/servers/orders/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("order", Order.find(Integer.parseInt(request.params("id"))));
      model.put("dishes", Dish.all());
      model.put("template", "templates/order.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    //Order - lost ingredients, cancel and restart i.e. diner sent it back
    post("/servers/orders/active/restart", (request, response) -> {
      Order thisOrder = Order.find(
        Integer.parseInt(request.queryParams("order-restart")));
      thisOrder.complete();
      Order newOrder = new Order(thisOrder.getTable(), thisOrder.getSeat(), thisOrder.getDishId());
      newOrder.save();
      newOrder.addComments(thisOrder.getComments());
      if (Dish.find(newOrder.getDishId()).hasEnoughIngredients()) {
        newOrder.make();
      }
      response.redirect("/servers/orders/" + newOrder.getId());
      return null;
    });

    //INGREDIENTS
    get("/manager/ingredients/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("ingredient", Ingredient.find(Integer.parseInt(request.params("id"))));
      model.put("template", "templates/ingredient.vtl");
      return new ModelAndView(model, layout);
      }, new VelocityTemplateEngine());


    // UPDATE ingredient name

    post("/manager/ingredients/:id/update", (request, response) -> {
      Ingredient ingredient = Ingredient.find(Integer.parseInt(request.params("id")));
      String newName = request.queryParams("new-name");
      ingredient.update(newName, ingredient.getUnit(), ingredient.getDesiredOnHand(), ingredient.getShelfLifeDays());
      response.redirect("/manager/ingredients/" + ingredient.getId());
      return null;
    });

    // UPDATE ingredient desired on hand

    post("/manager/ingredients/:id/update-stock", (request, response) -> {
      Ingredient ingredient = Ingredient.find(Integer.parseInt(request.params("id")));
      String newDesiredStock = request.queryParams("new-desired-stock");
      ingredient.update(ingredient.getName(), ingredient.getUnit(), Integer.parseInt(newDesiredStock), ingredient.getShelfLifeDays());
      response.redirect("/manager/ingredients/" + ingredient.getId());
      return null;
    });

    post("/manager/new-ingredient", (request, response) -> {
      Ingredient newIngredient = new Ingredient(
        request.queryParams("new-name"),
        request.queryParams("new-unit"),
        Integer.parseInt(request.queryParams("new-desired")),
        Integer.parseInt(request.queryParams("shelf-life-days")));
      newIngredient.save();
      response.redirect("/manager/inventory");
      return null;
    });

    //ADD new ingredient

    get("/manager/new-ingredient", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("template", "templates/ingredient-new.vtl");
      return new ModelAndView(model, layout);
      }, new VelocityTemplateEngine());

    //INVENTORY
    get("/manager/inventory", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("ingredients", Ingredient.all());
      model.put("dishes", Dish.all());
      model.put("eightysixes", Dish.getEightySixes());
      model.put("template", "templates/ingredients-inventory.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    //Take a delivery
    post("/manager/ingredients/inventory", (request, response) -> {
      for (Ingredient ingredient : Ingredient.all()) {
        int amount = Integer.parseInt(request.queryParams(ingredient.getName()));
        if (amount > 0) {
          Inventory delivery = new Inventory(ingredient.getId(), amount);
          delivery.save();
        }
      }
      response.redirect("/manager/inventory");
      return null;
    });

    get("/manager/delivery", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("ingredients", Ingredient.all());
      model.put("template", "templates/ingredients-delivery.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

// GET DISHES

    //DISHES

    get("/manager/dishes", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      Integer[] daysAgo = {1, 2, 3, 4, 5, 6, 7};
      Integer[] arrayIndex = {0, 1, 2, 3, 4, 5, 6};

      model.put("dishes", Dish.all());
      model.put("LocalDate", LocalDate.class);
      model.put("OrderClass", Order.class);
      model.put("daysAgo", daysAgo);
      model.put("arrayIndex", arrayIndex);
      model.put("orderPercents", Order.getOrderPercentsForWeek());
      model.put("template", "templates/dishes.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

    get("/manager/new-dish", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("template", "templates/dish-new.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

// GET DISH

    get("/manager/dishes/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("dish", Dish.find(Integer.parseInt(request.params("id"))));
      model.put("recipes", Recipe.all());
      model.put("ingredients", Ingredient.all());
      model.put("template", "templates/dish.vtl");
      return new ModelAndView(model, layout);
    }, new VelocityTemplateEngine());

// POST NEW DISH

    post("/manager/new-dish", (request, response) -> {
      Dish newDish = new Dish(request.queryParams("dish-name"), Integer.parseInt(request.queryParams("category-id")));
      newDish.save();
      response.redirect("/manager/dishes/" + newDish.getId());
      return null;
    });


// UPDATE DISH

    post("/manager/dishes/:id/update", (request, response) -> {
      Dish dish = Dish.find(Integer.parseInt(request.params("id")));
      String newName = request.queryParams("new-name");
      dish.update(newName, dish.getCategory());
      response.redirect("/manager/dishes/" + dish.getId());
      return null;
    });

    post("/manager/dishes/:id/add-ingredient", (request, response) -> {
      Dish dish = Dish.find(Integer.parseInt(request.queryParams("dish-id")));
      dish.addIngredient(Integer.parseInt(request.queryParams("add-ingredient")), Integer.parseInt(request.queryParams("amount")));
      response.redirect("/manager/dishes/" + dish.getId());
      return null;
    });

    post("/manager/dishes/:id/delete-ingredient", (request, response) -> {
      Dish dish = Dish.find(Integer.parseInt(request.params("id")));
      dish.removeIngredient(Integer.parseInt(request.queryParams("remove-ingredient")));
      response.redirect("/manager/dishes/" + dish.getId());
      return null;
    });

  }
}
