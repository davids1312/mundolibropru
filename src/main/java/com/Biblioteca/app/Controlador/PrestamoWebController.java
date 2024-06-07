package com.Biblioteca.app.Controlador;

import com.Biblioteca.app.Entidad.Libro;
import com.Biblioteca.app.Entidad.Prestamo;
import com.Biblioteca.app.Entidad.Usuario;
import com.Biblioteca.app.Repository.PrestamoRepository;
import com.Biblioteca.app.Repository.LibroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Controller

@RequestMapping("/prestamos")
public class PrestamoWebController {

    @Autowired
    private PrestamoRepository prestamoRepositorio;

    @Autowired
    private LibroRepository libroRepository;

    @GetMapping("/solicitar/{idLibro}")
    public String solicitarPrestamo(@PathVariable("idLibro") String idLibro, Model model) {
        model.addAttribute("idLibro", idLibro);
        return "prestamos";
    }

    @PostMapping("/guardar")
    public String guardarPrestamo(@RequestParam("idLibro") String idLibro,
                                  @RequestParam("fechaDevolucion") String fechaDevolucionStr,
                                  HttpSession session,
                                  Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        String idUsuario = usuario.getId();

        Date fechaDevolucion;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            fechaDevolucion = formatter.parse(fechaDevolucionStr);
        } catch (ParseException e) {
            model.addAttribute("error", "Formato de fecha inválido");
            return "prestamos";
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setIdUsuario(idUsuario);
        prestamo.setIdLibro(idLibro);
        prestamo.setFechaPrestamo(new Date());
        prestamo.setFechaDevolucion(fechaDevolucion);

        prestamoRepositorio.save(prestamo);

        Libro libro = libroRepository.findById(idLibro).orElse(null);
        if (libro != null) {
            int cantidadActual = libro.getCantidad();
            if (cantidadActual > 0) {
                libro.setCantidad(cantidadActual - 1);
                libroRepository.save(libro);
            } else {
                model.addAttribute("error", "No hay suficientes libros disponibles");
                return "prestamos";
            }
        }

        return "redirect:/prestamos";
    }

    @GetMapping
    public String verPrestamos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("usuario", usuario);

        List<Prestamo> prestamos = prestamoRepositorio.findByIdUsuario(usuario.getId()); // Filtrar por ID de usuario

        List<Libro> libros = new ArrayList<>();
        for (Prestamo prestamo : prestamos) {
            Libro libro = libroRepository.findById(prestamo.getIdLibro()).orElse(null);
            libros.add(libro);  // Agregar aunque sea null para mantener el índice correcto
        }

        model.addAttribute("prestamos", prestamos);
        model.addAttribute("libros", libros);
        
        return "prestamos";
    }
    
    @PostMapping("/devolver/{idPrestamo}")
    public String devolverPrestamo(@PathVariable("idPrestamo") String idPrestamo,
                                   HttpSession session,
                                   Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Prestamo prestamo = prestamoRepositorio.findById(idPrestamo).orElse(null);
        if (prestamo == null) {
            model.addAttribute("error", "No se encontró el préstamo");
            return "redirect:/prestamos";
        }

        Libro libro = libroRepository.findById(prestamo.getIdLibro()).orElse(null);
        if (libro != null) {
            libro.setCantidad(libro.getCantidad() + 1);
            libroRepository.save(libro);
        }

        prestamoRepositorio.delete(prestamo);

        return "redirect:/prestamos";
    }
}
